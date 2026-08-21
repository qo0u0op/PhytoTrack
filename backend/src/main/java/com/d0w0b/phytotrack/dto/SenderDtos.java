package com.d0w0b.phytotrack.dto;

/**
 * 送件人相關 DTO
 */
public final class SenderDtos {

  private SenderDtos() {
  }

  /** 送件人回應（供搜尋與管理） */
  public record SenderResponse(
      Long senderId,
      String name,
      String displayName,
      String phone,
      String address,
      Long districtId,
      String districtName,
      String cityName,
      Long senderTypeId,
      String senderTypeName) {
  }
}
