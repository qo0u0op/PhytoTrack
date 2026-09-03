package com.d0w0b.phytotrack.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 送件人相關 DTO
 */
public final class SenderDtos {

  private SenderDtos () {
  }

  /** 送件人回應 (供搜尋與管理) */
  public record SenderResponse (Long senderId,
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

  /** 送件人建立/更新請求：phone 與 displayName 至少一有值 (Service 檢查)；address 選填，空值存 null */
  public record SenderUpsertRequest (String name,
      String displayName,
      String phone,
      String address,
      @NotNull (message = "送件人鄉鎮市區不可為空") Long districtId,
      @NotNull (message = "身分別不可為空") Long senderTypeId) {
  }
}
