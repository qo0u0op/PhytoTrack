# PhytoTrack

`PhytoTrack` 是一個使用 `Spring Boot`、`Thymeleaf`、`Spring Data JPA` 與 `SQLite` 的簡易會員系統示範專案。

目前提供的功能：

- 首頁導覽
- 註冊
- 登入
- 登出

## 技術堆疊

- `Java 21`
- `Spring Boot 4`
- `Spring MVC`
- `Spring Data JPA`
- `Thymeleaf`
- `SQLite`
- `Lombok`
- `SweetAlert2`

## 專案結構

- `src/main/java/com/d0w0b/controller`：MVC Controller
- `src/main/java/com/d0w0b/service`：商業邏輯
- `src/main/java/com/d0w0b/repository`：資料存取層
- `src/main/java/com/d0w0b/models`：JPA Entity
- `src/main/resources/templates`：Thymeleaf 樣板
- `src/main/resources/static`：靜態資源

## 主要頁面

- 首頁：`/`
- 登入頁：`/user/login`
- 註冊頁：`/user/register`
- 登出：`/user/logout`

## 功能說明

### 註冊

- 使用者輸入 `username`、`displayName`、`password`
- 系統會檢查 `username` 是否重複
- 註冊成功後會顯示提示，並導向登入頁

### 登入

- 使用者輸入 `username` 與 `password`
- 系統會比對資料庫中的帳號密碼
- 登入成功後會把使用者資訊放進 `session`

### 登出

- 清除 `session`
- 回到首頁

## 資料庫

本專案使用本機 `SQLite`，資料庫檔案預設為：

- `./diagnoses.db`

JPA 設定位於：

- `src/main/resources/application.yaml`

Entity 設定位於：

- `src/main/java/com/d0w0b/models/User.java`

如果需要重建資料表，可以檢查：

- `src/main/resources/schema.sql`

## 啟動方式

1. 確認已安裝 `Java 21`
2. 執行：

```bash
./mvnw spring-boot:run
```

1. 開啟瀏覽器前往：

```text
http://localhost:8080
```

## 測試

執行：

```bash
./mvnw test
```

## TODO

- [ ] 改用 DTO，避免 Controller/表單直接綁定 `User` Entity
- [ ] 將登入狀態改為 Cookie + Session 以外的更明確登入識別設計
- [ ] 為 `User` Entity 補上明確的 `@Column(name = ...)` 對應
- [ ] 密碼改為雜湊儲存，後續加入 `BCrypt`、驗證機制與權限控管
