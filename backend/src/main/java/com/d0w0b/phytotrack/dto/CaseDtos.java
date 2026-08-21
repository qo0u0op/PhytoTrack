package com.d0w0b.phytotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 案件（Case）相關的資料傳輸物件（DTO）
 */
public final class CaseDtos {

  private CaseDtos() {
  }

  /** 案件建立請求 */
  public record CaseCreateRequest(
      @NotNull(message = "收件日期不可為空") LocalDate receiveDate,

      String cropScale,
      String damageScale,
      String pestDescription,
      String hintDescription,

      // 送件人（Sender）欄位：若提供 senderId 則沿用，否則依 name/phone/displayName 建立
      Long senderId,
      String senderName,
      String senderDisplayName,
      String senderPhone,
      @NotBlank(message = "送件人地址不可為空白") String senderAddress,
      @NotNull(message = "送件人鄉鎮市區不可為空") Long senderDistrictId,
      @NotNull(message = "送件人身分別不可為空") Long senderTypeId,

      // 參照資料（Reference Data）的 ID
      @NotNull(message = "耕種方式不可為空") Long methodId,
      @NotNull(message = "作物不可為空") Long cropId,
      @NotNull(message = "服務類別不可為空") Long serviceId,
      @NotNull(message = "送件方式不可為空") Long deliverId,

      // 多對多關聯（Junction Table）的 ID 集合
      List<Long> damageIds,
      List<Long> hintIds,
      List<Long> pestCategoryIds,
      List<Long> identifierIds) {

    // 相容舊版 18 參數建構（無 senderId/displayName）
    public CaseCreateRequest(LocalDate receiveDate, String cropScale, String damageScale,
        String pestDescription, String hintDescription,
        String senderName, String senderPhone, String senderAddress,
        Long senderDistrictId, Long senderTypeId,
        Long methodId, Long cropId, Long serviceId, Long deliverId,
        List<Long> damageIds, List<Long> hintIds, List<Long> pestCategoryIds, List<Long> identifierIds) {
      this(receiveDate, cropScale, damageScale, pestDescription, hintDescription,
          null, senderName, null, senderPhone, senderAddress, senderDistrictId, senderTypeId,
          methodId, cropId, serviceId, deliverId, damageIds, hintIds, pestCategoryIds, identifierIds);
    }
  }

  /** 案件更新請求（可局部更新） */
  public record CaseUpdateRequest(
      LocalDate receiveDate,
      String cropScale,
      String damageScale,
      String pestDescription,
      String hintDescription,
      String status,
      Long methodId,
      Long cropId,
      Long serviceId,
      Long deliverId,

      // 送件人（Sender）欄位：任一提供即更新案件關聯的送件人
      Long senderId,
      String senderName,
      String senderDisplayName,
      String senderPhone,
      String senderAddress,
      Long senderDistrictId,
      Long senderTypeId,

      // 多對多關聯：組非 null 時整組替換
      List<Long> damageIds,
      List<Long> hintIds,
      List<Long> pestCategoryIds,
      List<Long> identifierIds) {

    // 相容舊版 19 參數建構
    public CaseUpdateRequest(LocalDate receiveDate, String cropScale, String damageScale,
        String pestDescription, String hintDescription, String status,
        Long methodId, Long cropId, Long serviceId, Long deliverId,
        String senderName, String senderPhone, String senderAddress,
        Long senderDistrictId, Long senderTypeId,
        List<Long> damageIds, List<Long> hintIds, List<Long> pestCategoryIds, List<Long> identifierIds) {
      this(receiveDate, cropScale, damageScale, pestDescription, hintDescription, status,
          methodId, cropId, serviceId, deliverId,
          null, senderName, null, senderPhone, senderAddress, senderDistrictId, senderTypeId,
          damageIds, hintIds, pestCategoryIds, identifierIds);
    }
  }

  /** 案件列表篩選條件（查詢參數，皆可空） */
  public record CaseFilter(
      Long cropId,
      Long serviceId,
      String senderName,
      LocalDate receiveDateFrom,
      LocalDate receiveDateTo,
      String status) {

    /** 空篩選（等同不分條件） */
    public static CaseFilter empty() {
      return new CaseFilter(null, null, null, null, null, null);
    }

    /** 是否有任一條件 */
    public boolean isEmpty() {
      return cropId == null && serviceId == null && senderName == null
          && receiveDateFrom == null && receiveDateTo == null && status == null;
    }
  }

  /** 案件列表（摘要）回應：不帶多對多關聯，節省查詢量 */
  public record CaseSummaryResponse(
      Long caseId,
      LocalDate receiveDate,
      String cropName,
      String senderName,
      String senderDisplayName,
      String senderPhone,
      String senderAddress,
      Long senderId,
      Long senderDistrictId,
      String senderDistrictName,
      String senderCityName,
      String serviceName,
      String status,
      LocalDateTime createdAt) {

    // 相容舊版 7 參數建構
    public CaseSummaryResponse(Long caseId, LocalDate receiveDate, String cropName,
        String senderName, String serviceName, String status, LocalDateTime createdAt) {
      this(caseId, receiveDate, cropName, senderName, null, null, null, null, null, null, null,
          serviceName, status, createdAt);
    }
  }

  /** 案件詳細回應 */
  public record CaseResponse(
      Long caseId,
      LocalDate receiveDate,
      String cropScale,
      String damageScale,
      String pestDescription,
      String hintDescription,
      String status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      Long senderId,
      String senderName,
      String senderDisplayName,
      String senderPhone,
      String senderAddress,
      Long senderDistrictId,
      String senderDistrictName,
      String senderCityName,
      Long senderTypeId,
      String cropName,
      String methodName,
      String serviceName,
      String deliveryName,
      String createdByName,
      List<IdName> damages,
      List<IdName> hints,
      List<IdName> pestCategories,
      List<IdName> identifiers) {

    // 相容舊版 24 參數建構（無 senderId/displayName/city）
    public CaseResponse(Long caseId, LocalDate receiveDate, String cropScale, String damageScale,
        String pestDescription, String hintDescription, String status,
        LocalDateTime createdAt, LocalDateTime updatedAt,
        String senderName, String senderPhone, String senderAddress,
        Long senderDistrictId, String senderDistrictName, Long senderTypeId,
        String cropName, String methodName, String serviceName, String deliveryName,
        String createdByName, List<IdName> damages, List<IdName> hints,
        List<IdName> pestCategories, List<IdName> identifiers) {
      this(caseId, receiveDate, cropScale, damageScale, pestDescription, hintDescription, status,
          createdAt, updatedAt, null, senderName, null, senderPhone, senderAddress,
          senderDistrictId, senderDistrictName, null, senderTypeId, cropName, methodName,
          serviceName, deliveryName, createdByName, damages, hints, pestCategories, identifiers);
    }

    /** 通用的「ID + 名稱」結構，用於多對多關聯 */
    public record IdName(Long id, String name) {
    }
  }
}
