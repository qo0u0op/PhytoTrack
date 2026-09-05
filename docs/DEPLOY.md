# PhytoTrack 部署文件

本文說明如何在本機或 5 人區域網路 (LAN) 內部部署 PhytoTrack。

## 1. 環境需求

| 元件 | 版本 | 用途 |
|------|------|------|
| JDK | 21+ | 執行 Spring Boot 後端 |
| Node.js | 24 LTS (`mise.toml` 以 `node = "lts"` 鎖定 24.19.0，`mise.lock` 鎖定工具鏈) | 建置前端 (npm) 與 Playwright E2E |
| llama.cpp | 最新版 | 提供 AI 診斷 (`llama-server`) |
| GGUF 模型 | 任一繁體中文相容模型 | 如 `llama-3.2-3b-instruct` 或 `qwen2.5-3b-instruct` |

不需要安裝資料庫——後端使用檔案型 SQLite (`diagnoses.db`)，首次啟動自動建立資料表與種子資料。

## 2. 後端啟動

```bash
cd backend
mvn spring-boot:run                      # 已安裝 mise (mise 提供 maven 3.9.16)
# 或 (無 mise 時)
./mvnw spring-boot:run                   # Unix/macOS
.\mvnw.cmd spring-boot:run               # Windows
```

啟動成功後：

- API 服務：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

### 常用設定 (`backend/src/main/resources/application.yaml`)

| 設定 | 預設 | 說明 |
|------|------|------|
| `app.jwt.secret` | 開發用密鑰 | JWT 簽章密鑰。正式環境務必以 `JWT_SECRET` 環境變數覆蓋 |
| `app.bootstrap.*` | admin/admin123、staff/staff123、viewer/viewer123 | 首次啟動自動建立的管理者、診斷員與檢視者帳號 |
| `app.ai.health-url` | `http://localhost:11435/health` | llama-server 存活檢查端點 |
| `CORS_ALLOWED_ORIGINS` / `app.cors.allowed-origins` | 空（`dev`→`*`、`prod`→拒絕） | CORS 白名單，逗號分隔。例 `https://app.example.com,https://admin.example.com`。同源部署可空，跨源 `prod` 需明確配置，否則瀏覽器阻擋 |
| `app.rate-limit.enabled` | `true`（`test`→`false`） | 登入/註冊限流開關。`POST /api/auth/login|register|abandon-deactivate` 每 IP 10/min，超限 `429` + `Retry-After: 60` |
| `app.security-headers.enabled` | `false`（`prod`→`true`） | 安全標頭（`CSP / HSTS / nosniff / DENY`）開關。`prod` 自動注入，`dev` 不強制 |

驗證：

```bash
# CORS 白名單
curl -i -H "Origin: https://app.example.com" http://localhost:8080/api/cases   # 應含 Allow-Origin
curl -i -H "Origin: https://evil.com" http://localhost:8080/api/cases        # 應無 Allow-Origin
# 速率限制
for i in {1..11}; do curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"x","password":"y"}'; done  # 第 11 次 429
# 安全標頭（prod）
curl -i http://localhost:8080/api/cases | grep -i -E "Content-Security-Policy|Strict-Transport-Security|X-Content-Type-Options|X-Frame-Options"
```

## 3. llama-server 啟動 (AI 診斷)

```bash
# 以 OpenAI 相容模式啟動，port 用 11435 避開 Spring Boot 的 8080
llama-server -m /path/to/model.gguf --port 11435
```

確認連線：

```bash
curl http://localhost:11435/health        # 回傳 ok
curl http://localhost:11435/v1/models     # 列出模型名稱 (須與後端 model 設定一致)
```

若未啟動 llama-server，系統其餘功能 (案件管理、登入) 仍可正常使用，僅 AI 診斷無法執行。

## 4. 前端建置與啟動

開發模式 (Vite dev server，含 `/api` 代理至後端 8080)：

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

正式部署 (產出靜態檔)：

```bash
cd frontend
npm run build
npm run preview          # 本機預覽 http://localhost:4173
# 或將 dist/ 交由 nginx 等靜態伺服器代管
```

若前後端分開部署，需把 `frontend/vite.config.ts` 的 proxy 目標改為後端實際位址，或在 nginx 設定 `/api` 反向代理至後端。

## 5. 區域網路 (LAN) 部署 (5 人)

1. 後端以 `0.0.0.0` 監聽：`mvn spring-boot:run -Dspring-boot.run.arguments=--server.address=0.0.0.0` (或 `./mvnw ...` (Unix/macOS)/ `.\mvnw.cmd ...` (Windows)，無 mise 時)
2. 前端 build 後以 nginx 服務 `dist/`，並將 `/api` 反向代理至後端 8080
3. 各用戶端瀏覽器開啟前端位址 (例如 `http://伺服器IP/`)
4. 防火牆需放行前端對外 port 與後端 8080 (若代理已設定，用戶端通常只需連前端 port)

### 部署前檢查清單

- [ ] 已設定強固的 `JWT_SECRET` (勿用預設值)
- [ ] 已變更 bootstrap 預設帳號密碼
- [ ] 後端 `diagnoses.db` 有定期備份
- [ ] llama-server 已隨開機啟動 (如需 AI 診斷)

## 6. 備份與還原

SQLite 為單檔資料庫，定期備份可降低誤刪或損壞風險。

### 備份

```bash
bash scripts/backup.sh
# 備份完成：/path/to/PhytoTrack/backups/phytotrack-20250827-143000.db
ls backups/
```

- 腳本會在專案根建立 `backups/` 目錄 (已於 `.gitignore` 忽略，不納版控)
- 來源優先 `backend/diagnoses.db`，其次 `diagnoses.db`；不存在時回非零並提示
- 檔名含本地時間戳 `YYYYmmdd-HHMMSS`，排序即時間排序
- 建議頻率：每日一次或每次部署前執行；可加入 `cron` 排程

### 還原

1. 停止後端服務 (避免寫入中複製)
2. 複製備份檔覆蓋原庫：

```bash
cp backups/phytotrack-YYYYmmdd-HHMMSS.db backend/diagnoses.db
# 或專案根的 diagnoses.db，依實際部署路徑而定
```

3. 重新啟動後端

> 備份目錄未納版控，請自行保留異地或雲端備份。

## 7. 既有資料庫遷移 (田區位置 `field_district_id`)

本版本 `cases.field_district_id` 改為 `NOT NULL` 且篩選視圖 `v_case_search` 改以田區位置為準。`CREATE TABLE IF NOT EXISTS` 不會為既有 `backend/diagnoses.db` 補欄位，需手動遷移：

```bash
# 1. 備份
bash scripts/backup.sh
# 2. 執行遷移 (自動：補欄位 → 80% 同送件人、20% 同縣市他鄉鎮 → 重建為 NOT NULL → 重建視圖)
bash scripts/migrate-field-district.sh
# 或手動 sqlite3 (見腳本內 SQL)
sqlite3 backend/diagnoses.db "SELECT COUNT (*) FROM cases WHERE field_district_id IS NULL;"
```

測試庫 `backend/target/phytotrack-test.db` 為產物，刪除 `rm target/phytotrack-test.db` 後 `mvn test` 會依新 `schema.sql` 重建。

## 7.1 既有資料庫遷移 (送件人地址 `senders.address` 放寬可空)

本版本 `senders.address` 改為可空（無地址送件可建檔，空值存 null）。`CREATE TABLE IF NOT EXISTS` 與 Hibernate `ddl-auto: update` 皆不會為既有 `backend/diagnoses.db` 放寬既有欄位的 `NOT NULL`，需手動遷移（SQLite 不支援直接 `ALTER COLUMN`，需建表搬資料）：

```bash
# 1. 備份
bash scripts/backup.sh
# 2. 建表搬資料 (保留既有地址，僅放寬約束)
sqlite3 backend/diagnoses.db <<'SQL'
ALTER TABLE senders RENAME TO senders_old;
CREATE TABLE senders (
  sender_id      INTEGER PRIMARY KEY,
  name           TEXT,
  display_name   TEXT,
  phone          TEXT,
  address        TEXT,
  district_id    INTEGER NOT NULL REFERENCES districts(district_id),
  sender_type_id INTEGER NOT NULL REFERENCES sender_types(sender_type_id)
);
INSERT INTO senders SELECT * FROM senders_old;
DROP TABLE senders_old;
CREATE INDEX IF NOT EXISTS idx_senders_district_id    ON senders(district_id);
CREATE INDEX IF NOT EXISTS idx_senders_sender_type_id ON senders(sender_type_id);
CREATE INDEX IF NOT EXISTS idx_senders_phone ON senders(phone);
SQL
```

測試庫同第 7 節刪除重建即可。


## 7.2 既有資料庫驗證 (`identifiers.former_user_id` 歷史欄位)

本版本 `identifiers` 新增可空 `former_user_id` 外鍵（記錄解綁前所屬使用者，供升權／啟用恢復原筆）。Hibernate `ddl-auto: update` 可自動 `ADD COLUMN`（SQLite 支援），既有 `backend/diagnoses.db` 重啟後自動補欄，無需手動遷移；`schema.sql` 已同步（新庫直接生效）。部署後驗證：

```bash
sqlite3 backend/diagnoses.db "PRAGMA table_info (identifiers);"
# 應含 former_user_id 欄；缺失時手動補：
sqlite3 backend/diagnoses.db "ALTER TABLE identifiers ADD COLUMN former_user_id INTEGER REFERENCES users(user_id);"
```

## 7.3 業務初始基準與縣市鄉鎮管理

業務初始 `schema-baseline.sql`（表結構＋參照種子，不含作物／業務資料）為測試／開發建庫基準：`application-test.yaml` 以 `schema-locations: classpath:schema-baseline.sql` 明確指定；開發庫（`backend/diagnoses.db`）以 `schema.sql`（已同步為業務初始）重建。縣市鄉鎮可經參照資料管理頁增改刪（`ADMIN`，被引用回 `409`）。

## 7.4 既有資料庫殘留 (`cities/districts.sort_order` 孤兒欄位)

本版本移除縣市鄉鎮 `sort_order` 欄位並改依 `id` 排序（`GET /api/ref/cities` 回應不再含 `sortOrder`）。Hibernate `ddl-auto: update` 不會刪除舊欄，既有 `backend/diagnoses.db` 的殘留欄位無害（JPA 不再映射），無需動作；新庫直接無該欄。如需清理可建表搬資料（參照 7.1 模式，非強制）。

## 8. CSV 匯出欄位順序變更 (BREAKING)

本版本依 `docs/diagnoses.typ` 紙本邏輯與後續 7+2 項調整重排 `GET /api/cases/export` 的欄位順序。舊版以欄位索引解析者請改以表頭解析。

**2026-08 前：**
`案件編號,收件日期,狀態,送件人,電話,縣市鄉鎮,地址,身分別,作物,種植面積,被害面積,被害部位,病蟲害,土壤栽培用藥紀錄,防治建議,簽名人,耕種方式,服務,交付,建立時間,更新時間`

**2026-09-01 起（align-case-field-order，Q1-Q5）：**
`收件編號,收件日期,狀態,病蟲害發生地點_縣市,病蟲害發生地點_鄉鎮,是否同寄件人,送件人身分別,姓名,顯示名稱,電話,住址,耕作方式,作物種類,作物名稱,被害部位,土壤栽培用藥紀錄,栽培面積,被害面積,被害描述,服務類別,送件方式,鑑定者,病害,蟲害,有害動物,生理因子,其他,建議事項,防治描述,建立者,建立時間,更新時間`

**2026-09-02 起（csv-export-format + case-display-filter-export）：**
`收件編號,收件日期,狀態,田區位置,身分別,姓名,顯示名稱,電話,住址,服務類別,送件方式,耕作方式,作物種類,作物名稱,被害部位,栽培面積,被害面積,土壤栽培用藥紀錄,病害,蟲害,有害動物,生理因子,其他,診斷結果,建議事項,防治描述,鑑定者,建立者,建立時間,更新時間`（全欄位 `"` 引號、狀態中文、收件編號 asc、篩選穿透）

| 舊欄位 | 新欄位 | 備註 |
|---|---|---|
| 案件編號 | 收件編號 | 更名 |
| 縣市鄉鎮 | 病蟲害發生地點_縣市/鄉鎮 → 田區位置 | 由送件人地址改為田區位置，後合併單欄 |
| 身分別 | 送件人身分別 → 身分別 | 同義，後續更回身分別 |
| 作物 | 作物種類 + 作物名稱 | 新增種類 (Q5) |
| 病蟲害 | 病害/蟲害/有害動物/生理因子/其他 五欄 | 拆分，`pestNote` 以 `名稱 (備註)` |
| 防治建議 | 建議事項 | 更名，第 6 項「其他」→「其他回覆」 (Q4) |
| - | 被害描述 → 診斷結果 | 更名並前移至建議事項前 |
| - | 是否同寄件人 | 曾新增，後移除（2026-09-02） |
| - | 栽培/被害面積 | 前移至土壤紀錄前（2026-09-02） |

影響：下游若以索引解析 CSV，需改以表頭名稱解析；全欄位已改為引號字串。

## 9. 監控與日誌 (Phase 2, api-observability)

- 健康檢查：`GET /actuator/health`（公開，`show-details: never` 僅回 `{"status":"UP"}`）、`GET /actuator/info`（公開，空物件）；其餘端點（含 `metrics`）不暴露（`management.endpoints.web.exposure.include=health,info`），勿改為 `*`
- 日誌：`logs/phytotrack.log` 為主檔，依日與大小滾動為 `logs/phytotrack.%d{yyyy-MM-dd}.%i.log`（`maxFileSize 10MB`、`maxHistory 30`、`totalSizeCap 1GB`），pattern 含 `[%X{requestId}]` 供追蹤（含 `RATE_LIMITED` 警告）；`logs/` 已 gitignore，建議納入備份排除
- 驗證：`curl http://localhost:8080/actuator/health` 應回 `{"status":"UP"}`；`ls logs/` 觀察滾動；`grep <requestId> logs/phytotrack.log` 追溯請求

## 10. 安全加固 (Phase 2, security-review)

- CORS 白名單：`CORS_ALLOWED_ORIGINS` 控制 `Access-Control-Allow-Origin`。`dev` 未配置沿用 `*`，`prod` 未配置預設拒絕（同源不受影響）。詳見 `application.yaml` 與 ADR-012。
- 速率限制：`POST /api/auth/login|register|abandon-deactivate` 每 IP 10/min，超限 `429` + `Retry-After: 60` + `error.code=RATE_LIMITED` + `requestId`，日誌 `log.warn` 可追溯。`test` 預設關閉。
- 安全標頭：`prod` 自動注入 `Content-Security-Policy`（`style-src 'unsafe-inline'` 相容 Swagger）、`Strict-Transport-Security`、`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`。見 ADR-012。
- Token 儲存：維持 `localStorage`（無 XSS 面，遷移 `httpOnly` 需恢復 CSRF，見 ADR-012）。

## 11. 升級到 PostgreSQL (選用)

現階段使用 SQLite 起步 (理由見 ADR-007)。若未來資料量與並發需求增加，切換方式：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres  # 或 ./mvnw ... (Unix/macOS)/ .\mvnw.cmd ... (Windows)，無 mise 時
```

搭配 `application-postgres.yaml` 設定連線資訊，並使用既有 schema 資料 (`schema.sql` 的 `INSERT` 語句相容 PostgreSQL)。

## 12. Binary 交付（Windows Portable / Unix XDG）

### 目錄契約

| 平台 | 配置 | 資料 | 日誌 | Binary 位置 |
|------|------|------|------|-------------|
| Windows Portable | `.\config\phytotrack.toml` | `.\data\diagnoses.db` | `.\logs\phytotrack.log` | 解壓目錄的 `phytotrack.exe` |
| Unix XDG | `$XDG_CONFIG_HOME/phytotrack/phytotrack.toml`（預設 `~/.config/...`） | `$XDG_DATA_HOME/phytotrack/diagnoses.db`（`~/.local/share/...`） | `$XDG_STATE_HOME/phytotrack/phytotrack.log`（`~/.local/state/...`） | `/usr/bin/phytotrack`（`apt/brew`） |

- 首次啟動若 `phytotrack.toml` 不存在，自動生成預設（含註解、全量鍵、`server.port=8080`、`ai.enabled=true`、`app.jwt.secret=<random>`），並 `mkdirs` 對應目錄；二次啟動不覆蓋。
- `app.jwt.secret` 首次亂數 48 bytes Base64URL，console 印「首次啟動已生成亂數密鑰，舊 token 失效請重新登入」。
- 僅 `AI_API_KEY` 支援 `env AI_API_KEY` 覆蓋 TOML，其餘走 `phytotrack.toml`；`backend/.env` 已棄用（僅相容一版，啟動印 WARN）。

### 單一配置 `phytotrack.toml`

見 `backend/phytotrack.toml.example`，全量可配：`server.port`、`spring.datasource.url`、`logging.file.name`、`app.jwt.secret/expiration-ms`、`app.bootstrap.*`、`ai.base-url/model/api-key/enabled`、`app.cors.allowed-origins`、`app.rate-limit.*`、`app.security-headers.enabled`、`springdoc`、`app.ui.auto-open-browser`。

```bash
# 改 port
# phytotrack.toml: server.port=9090
curl http://localhost:9090/actuator/health # 應回 UP
```

### Windows 防火牆與自動開瀏覽器

- 防火牆：首次啟動偵測 `netsh advfirewall firewall show rule name="PhytoTrack"`，未放行即 console 印 `netsh advfirewall firewall add rule ...` PowerShell 提示，不自動提權；請勿解壓至 `C:\Program Files`（無寫入權限）。
- 自動開瀏覽器：`ApplicationReadyEvent` 後 `Desktop.browse(http://localhost:${server.port})`，失敗回落 `xdg-open`/`open`，可由 `app.ui.auto-open-browser=false` 關閉。

### Unix 包管理器

```bash
# 例：deb 安裝後
phytotrack # 讀 ~/.config/phytotrack/phytotrack.toml，系統級 /etc/phytotrack/phytotrack.toml 為低優先
systemctl --user enable phytotrack # 若提供 systemd unit
```