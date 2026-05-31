-- 會員資料表，對應 register.html 表單欄位
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    password TEXT NOT NULL,
    email TEXT UNIQUE,
    role TEXT NOT NULL DEFAULT 'ROLE_VIEWER',
    active INTEGER NOT NULL DEFAULT 1
);
