package com.d0w0b.phytotrack.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 啟動時確保 senders.address 可為空
 *
 * 歷史 schema 為 NOT NULL，但 case-sender-address-nullable 已改為 nullable。
 * SQLite 的 CREATE TABLE IF NOT EXISTS 與 ddl-auto:update 不會自動放寬既有欄位，
 * 因此在此以 PRAGMA 檢測並在必要時重建表（保留資料，僅改約束）。
 */
@Component
public class SenderAddressMigration {

  private static final Logger log = LoggerFactory.getLogger (SenderAddressMigration.class);

  private final DataSource dataSource;

  public SenderAddressMigration (DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void migrateIfNeeded () {
    try (var conn = dataSource.getConnection ();
         var stmt = conn.createStatement ()) {
      // 檢查 address 是否仍為 NOT NULL
      boolean needsMigration = false;
      try (var rs = stmt.executeQuery ("PRAGMA table_info(senders)")) {
        while (rs.next ()) {
          String name = rs.getString ("name");
          int notnull = rs.getInt ("notnull");
          if ("address".equals (name) && notnull == 1) {
            needsMigration = true;
            break;
          }
        }
      }
      if (!needsMigration) return;

      log.info ("偵測到 senders.address 仍為 NOT NULL，執行遷移放寬為可空");
      stmt.execute ("PRAGMA foreign_keys=OFF");
      stmt.execute ("BEGIN TRANSACTION");
      stmt.execute ("ALTER TABLE senders RENAME TO senders_old");
      stmt.execute ("""
          CREATE TABLE senders (
            sender_id      INTEGER PRIMARY KEY,
            name           TEXT,
            display_name   TEXT,
            phone          TEXT,
            address        TEXT,
            district_id    INTEGER NOT NULL REFERENCES districts(district_id),
            sender_type_id INTEGER NOT NULL REFERENCES sender_types(sender_type_id)
          )
          """);
      stmt.execute ("INSERT INTO senders (sender_id, name, display_name, phone, address, district_id, sender_type_id) SELECT sender_id, name, display_name, phone, address, district_id, sender_type_id FROM senders_old");
      stmt.execute ("DROP TABLE senders_old");
      stmt.execute ("CREATE INDEX IF NOT EXISTS idx_senders_district_id ON senders(district_id)");
      stmt.execute ("CREATE INDEX IF NOT EXISTS idx_senders_sender_type_id ON senders(sender_type_id)");
      stmt.execute ("CREATE INDEX IF NOT EXISTS idx_senders_phone ON senders(phone)");
      stmt.execute ("COMMIT");
      stmt.execute ("PRAGMA foreign_keys=ON");
      log.info ("senders.address 遷移完成，已放寬為可空");
    } catch (Exception e) {
      log.warn ("senders.address 遷移失敗，將由後續操作報錯提示手動遷移", e);
    }
  }
}
