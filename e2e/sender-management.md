# 送件人管理 E2E 驗證 (playwright-cli / terminal-browser)

> 前置：`mise run dev` 已啟動前後端 (`http://localhost:5173` / `http://localhost:8080`)

## 1. STAFF 可於建案表單 inline 新增簽名人與作物

```bash
playwright-cli open http://localhost:5173/login
playwright-cli fill e3 "staff" --submit
# 假設登入後導向 /cases
playwright-cli goto http://localhost:5173/cases/new
playwright-cli snapshot
# 應可見「＋新增簽名人」與「＋新增作物」按鈕 (STAFF 可見)
playwright-cli click eXX --label="＋新增簽名人"
playwright-cli fill eYY "測試簽名人E2E"
playwright-cli click eZZ --label="新增"
playwright-cli snapshot --find="測試簽名人E2E"
```

## 2. 建案時送件人去重候選與獨立儲存

```bash
# 在送件人區塊輸入已存在姓名的一部分
playwright-cli fill eAA "王小明"
# 應自動彈「有相似的資料，是否帶入?」候選彈窗
playwright-cli snapshot --find="帶入"
playwright-cli click eBB --label="帶入"
# 應選用既有送件人，form.senderId 被設
# 修改電話後應出現「更新送件人」「取消編輯」按鈕
playwright-cli fill eCC "0912999999"
playwright-cli snapshot --find="更新送件人"
playwright-cli click eDD --label="更新送件人"
# 診斷區段應在送件人儲存後才可見
playwright-cli snapshot --find="土壤、栽培、用藥紀錄"
```

## 3. 編輯既有案件時可更新送件人 (先前 bug 驗證)

```bash
playwright-cli goto http://localhost:5173/cases/1/edit
playwright-cli snapshot
# 應可見送件人區塊的「搜尋候選」與「儲存送件人」/「更新送件人」
playwright-cli fill eEE "李四"
playwright-cli click eFF --label="搜尋候選"
playwright-cli snapshot --find="選擇送件人候選"
# 應可選候選或建立新
```

## 4. STAFF 可編輯送件人但不可刪除 (漢堡選單)

```bash
# STAFF 登入
playwright-cli open http://localhost:5173/admin/senders
playwright-cli snapshot
# 應可見表格與「編輯」按鈕，刪除按鈕不可見
playwright-cli find "編輯"
playwright-cli find "刪除" # 應無結果或僅 ADMIN 可見
# ADMIN 登入後應可見刪除
playwright-cli open http://localhost:5173/login
playwright-cli fill e3 "admin"
playwright-cli fill e5 "admin123"
playwright-cli click e12
playwright-cli goto http://localhost:5173/admin/senders
playwright-cli snapshot --find="刪除"
```

## 5. terminal-browser 互動式驗證

```bash
terminal-browser open http://localhost:5173/cases/new --split right
terminal-browser action -- snapshot
terminal-browser action -- fill @e3 "staff"
```

> 以上 ref (eXX) 皆為範例，實務上每次 snapshot 後以 find 定位正確 ref。
