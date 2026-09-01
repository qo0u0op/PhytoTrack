#!/usr/bin/env bash
# migrate-field-district.sh — 將既有 diagnoses.db 的 field_district_id 補齊並改為 NOT NULL，並重建 v_case_search 為田區語意
set -euo pipefail
DB="${1:-backend/diagnoses.db}"
if [ ! -f "$DB" ]; then
  echo "找不到 DB：$DB" >&2
  exit 1
fi
echo "遷移 DB：$DB"

sqlite3 "$DB" <<SQL
PRAGMA foreign_keys=off;
DROP VIEW IF EXISTS v_case_search;
-- 若欄位不存在，先以可空新增（冪等，失敗忽略）
SQL
# 嘗試新增欄位，若已存在則忽略錯誤
sqlite3 "$DB" "ALTER TABLE cases ADD COLUMN field_district_id INTEGER REFERENCES districts(district_id);" 2>/dev/null || true

# 80% 同送件人、20% 同縣市他鄉鎮回填
python3 - "$DB" <<'PY'
import sqlite3, random, sys
from collections import defaultdict
db_path = sys.argv[1]
conn = sqlite3.connect(db_path)
cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM cases WHERE field_district_id IS NULL")
null_cnt = cur.fetchone()[0]
if null_cnt and null_cnt > 0:
    cur.execute("SELECT c.case_id, s.district_id, d.city_id FROM cases c JOIN senders s ON s.sender_id=c.sender_id JOIN districts d ON d.district_id=s.district_id ORDER BY c.case_id")
    rows = cur.fetchall()
    cur.execute("SELECT district_id, city_id FROM districts")
    city_to_districts = defaultdict(list)
    for did, cid in cur.fetchall():
        city_to_districts[cid].append(did)
    random.seed(456)
    indices = list(range(len(rows)))
    random.shuffle(indices)
    diff_n = len(rows) // 5  # 20%
    diff_set = set(indices[:diff_n])
    updates = []
    for idx, (case_id, sender_did, city_id) in enumerate(rows):
        cur.execute("SELECT field_district_id FROM cases WHERE case_id=?", (case_id,))
        existing = cur.fetchone()[0]
        if existing is not None:
            continue
        if idx in diff_set:
            cands = [d for d in city_to_districts[city_id] if d != sender_did]
            field = random.choice(cands) if cands else sender_did
        else:
            field = sender_did
        updates.append((field, case_id))
    if updates:
        cur.executemany("UPDATE cases SET field_district_id=? WHERE case_id=?", updates)
        conn.commit()
        print(f"回填 {len(updates)} 筆（原 null {null_cnt}）")
    else:
        print("無需回填")
else:
    print("field_district_id 已全數有值，無需回填")
cur.execute("SELECT COUNT(*) FROM cases WHERE field_district_id IS NULL")
if cur.fetchone()[0] != 0:
    print("錯誤：仍有 NULL，無法改為 NOT NULL", file=sys.stderr)
    sys.exit(1)
conn.close()
PY

# 重建為 NOT NULL（若已為 NOT NULL 則跳過）
NEED=$(sqlite3 "$DB" "SELECT CASE WHEN (SELECT sql FROM sqlite_master WHERE type='table' AND name='cases') LIKE '%field_district_id INTEGER NOT NULL%' THEN 'no' ELSE 'yes' END;")
if [ "$NEED" = "yes" ]; then
sqlite3 "$DB" <<SQL
PRAGMA foreign_keys=off;
BEGIN TRANSACTION;
CREATE TABLE cases_new (
  case_id          INTEGER PRIMARY KEY,
  receive_date     DATE    NOT NULL,
  crop_scale       TEXT,
  damage_scale     TEXT,
  case_description TEXT,
  hint_description TEXT,
  status           INTEGER NOT NULL DEFAULT 0,
  created_at       TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
  updated_at       TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
  sender_id        INTEGER NOT NULL REFERENCES senders(sender_id),
  method_id        INTEGER NOT NULL REFERENCES methods(method_id),
  crop_id          INTEGER NOT NULL REFERENCES crops(crop_id),
  service_id       INTEGER NOT NULL REFERENCES services(service_id),
  deliver_id       INTEGER NOT NULL REFERENCES deliveries(deliver_id),
  field_district_id INTEGER NOT NULL REFERENCES districts(district_id),
  created_by       INTEGER NOT NULL REFERENCES users(user_id)
);
INSERT INTO cases_new (case_id, receive_date, crop_scale, damage_scale, case_description, hint_description, status, created_at, updated_at, sender_id, method_id, crop_id, service_id, deliver_id, field_district_id, created_by)
SELECT case_id, receive_date, crop_scale, damage_scale, case_description, hint_description, status, created_at, updated_at, sender_id, method_id, crop_id, service_id, deliver_id, field_district_id, created_by FROM cases;
DROP TABLE cases;
ALTER TABLE cases_new RENAME TO cases;
CREATE INDEX idx_cases_sender_id ON cases(sender_id);
CREATE INDEX idx_cases_field_district_id ON cases(field_district_id);
CREATE INDEX idx_cases_status ON cases(status);
CREATE INDEX idx_cases_receive_date ON cases(receive_date);
COMMIT;
PRAGMA foreign_keys=on;
SQL
echo "cases 表已重建為 NOT NULL"
else
echo "cases 表已為 NOT NULL，跳過重建"
fi

# 重建視圖為田區語意
sqlite3 "$DB" <<SQL
DROP VIEW IF EXISTS v_case_search;
CREATE VIEW v_case_search AS
SELECT
  c.case_id,
  c.receive_date,
  c.status,
  c.created_at,
  s.name AS sender_name,
  s.display_name AS sender_display_name,
  s.phone AS sender_phone,
  fd.district_id AS district_id,
  fd.city_id AS city_id,
  cr.crop_id,
  cc.crop_category_id,
  c.service_id,
  c.deliver_id,
  c.method_id,
  CAST(COUNT(DISTINCT cpc.cpc_id) AS INTEGER) AS pest_category_count,
  CAST(REPLACE(GROUP_CONCAT(DISTINCT pc.pest_category), ',', '、') AS TEXT) AS pest_category_names,
  CAST(REPLACE(GROUP_CONCAT(DISTINCT h.hint), ',', '、') AS TEXT) AS hint_names,
  CAST(REPLACE(GROUP_CONCAT(DISTINCT dm.damage), ',', '、') AS TEXT) AS damage_names
FROM cases c
LEFT JOIN senders s ON s.sender_id = c.sender_id
LEFT JOIN districts fd ON fd.district_id = c.field_district_id
LEFT JOIN crops cr ON cr.crop_id = c.crop_id
LEFT JOIN crop_categories cc ON cc.crop_category_id = cr.crop_category_id
LEFT JOIN case_pest_categories cpc ON cpc.case_id = c.case_id
LEFT JOIN pest_categories pc ON pc.pest_category_id = cpc.pest_category_id
LEFT JOIN case_hints ch ON ch.case_id = c.case_id
LEFT JOIN hints h ON h.hint_id = ch.hint_id
LEFT JOIN case_damages cd ON cd.case_id = c.case_id
LEFT JOIN damages dm ON dm.damage_id = cd.damage_id
GROUP BY c.case_id, c.receive_date, c.status, c.created_at, s.name, s.display_name, s.phone, fd.district_id, fd.city_id, cr.crop_id, cc.crop_category_id, c.service_id, c.deliver_id, c.method_id;
SQL
echo "v_case_search 已重建為田區語意"
sqlite3 "$DB" "PRAGMA foreign_key_check;"
echo "遷移完成：$DB"
