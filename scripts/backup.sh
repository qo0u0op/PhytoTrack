#!/bin/sh
# PhytoTrack SQLite 備份腳本
# 將 diagnoses.db 複製為帶時間戳的備份檔
# 用法： bash scripts/backup.sh  或  ./scripts/backup.sh

set -eu

# 解析專案根（腳本位於 <root>/scripts/backup.sh）
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# 來源優先 backend/diagnoses.db，其次專案根 diagnoses.db
SRC=""
if [ -f "$ROOT_DIR/backend/diagnoses.db" ]; then
  SRC="$ROOT_DIR/backend/diagnoses.db"
elif [ -f "$ROOT_DIR/diagnoses.db" ]; then
  SRC="$ROOT_DIR/diagnoses.db"
else
  echo "錯誤：找不到資料庫檔案（backend/diagnoses.db 或 diagnoses.db）" >&2
  exit 1
fi

# 目標目錄與檔名（本地時間，排序即時間排序）
BACKUP_DIR="$ROOT_DIR/backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
DST="$BACKUP_DIR/phytotrack-$TIMESTAMP.db"

if ! cp -- "$SRC" "$DST"; then
  echo "錯誤：複製失敗 $SRC -> $DST" >&2
  exit 1
fi

echo "備份完成：$DST"
