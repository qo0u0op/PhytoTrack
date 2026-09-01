package com.d0w0b.phytotrack.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/**
 * LocalDate 屬性轉換器 (Attribute Converter)
 *
 * 將 LocalDate 以 ISO 字串 (yyyy-MM-dd) 存入 SQLite。
 * 原因：Hibernate 7 的 SQLiteDialect 對 LocalDate/LocalDateTime
 * 會寫入 epoch 毫秒數字，但讀取時 sqlite-jdbc 卻以嚴格時間格式解析，
 * 造成「Error parsing time stamp」。改用字串轉換器可繞過此缺陷。
 *
 * autoApply = true：自動套用到所有 LocalDate 欄位，不需個別加 @Convert。
 */
@Converter (autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

  @Override
  public String convertToDatabaseColumn (LocalDate attribute) {
    return attribute == null ? null : attribute.toString ();
  }

  @Override
  public LocalDate convertToEntityAttribute (String dbData) {
    return dbData == null ? null : LocalDate.parse (dbData);
  }
}