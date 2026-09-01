package com.d0w0b.phytotrack.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * LocalDateTime 屬性轉換器 (Attribute Converter)
 *
 * 將 LocalDateTime 以「yyyy-MM-dd HH:mm:ss[.SSS]」字串存入 SQLite，
 * 與 SQLite 的 datetime ('now','localtime') 輸出格式相容。
 * 原因：Hibernate 7 的 SQLiteDialect 對 LocalDateTime 寫入 epoch 毫秒，
 * 讀取時 sqlite-jdbc 卻以嚴格格式解析而失敗 (見 LocalDateConverter)。
 *
 * autoApply = true：自動套用到所有 LocalDateTime 欄位。
 */
@Converter (autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

  /** 接受「yyyy-MM-dd HH:mm:ss」與「yyyy-MM-dd HH:mm:ss.SSS」兩種格式 */
  private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder ()
      .appendPattern ("yyyy-MM-dd HH:mm:ss")
      .optionalStart ()
      .appendFraction (ChronoField.NANO_OF_SECOND, 0, 9, true)
      .optionalEnd ()
      .toFormatter ();

  @Override
  public String convertToDatabaseColumn (LocalDateTime attribute) {
    return attribute == null ? null : attribute.format (FORMATTER);
  }

  @Override
  public LocalDateTime convertToEntityAttribute (String dbData) {
    return dbData == null ? null : LocalDateTime.parse (dbData, FORMATTER);
  }
}