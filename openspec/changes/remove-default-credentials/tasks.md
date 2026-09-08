## 1. 設定檔去明文密碼

- [x] 1.1 `config/PhytotrackTomlEnvironmentPostProcessor.java:generateDefaultToml` 將 `[app.bootstrap]` 改為純註釋（不含任何有效 `*-username`/`*-password` 行），`phytotrack.toml.example` 同步，驗證 `grep -E "^admin-username|^admin-password" backend/phytotrack.toml.example` 為 0 且生成檔僅含註釋
- [x] 1.2 保留 `service/DataInitializer.java` prod `admin/admin123` 建號與 `application.yaml` 預設值不變，確認 `mvn test` 登入 `admin/admin123` 仍成功

## 2. 文件與驗證

- [x] 2.1 `docs/DEPLOY.md` 配置表、`README.md` 移除 TOML 帳密配置並說明首啟後請修改密碼（`phytotrack.toml` 僅註釋提醒），`openspec validate --specs --changes` 通過
