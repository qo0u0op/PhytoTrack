package com.d0w0b.phytotrack.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 案件 (Case) 相關的資料傳輸物件 (DTO)
 */
public final class CaseDtos {

  private CaseDtos () {
  }

  /** 案件建立請求 */
  public record CaseCreateRequest (@NotNull (message = "收件日期不可為空") LocalDate receiveDate,

      String cropScale,
      String damageScale,
      String caseDescription,
      String hintDescription,

      // 送件人 (Sender) 欄位：若提供 senderId 則沿用，否則依 name/phone/displayName 建立
      Long senderId,
      String senderName,
      String senderDisplayName,
      String senderPhone,
      // 送件人地址選填：空值（未傳/null/全空白）視為未綁定地址，存為 null
      String senderAddress,
      @NotNull (message = "送件人鄉鎮市區不可為空") Long senderDistrictId,
      @NotNull (message = "身分別不可為空") Long senderTypeId,

      // 參照資料 (Reference Data) 的 ID
      @NotNull (message = "耕種方式不可為空") Long methodId,
      @NotNull (message = "作物不可為空") Long cropId,
      @NotNull (message = "服務類別不可為空") Long serviceId,
      @NotNull (message = "送件方式不可為空") Long deliverId,

      @NotNull (message = "田區位置不可為空") Long fieldDistrictId,

      // 多對多關聯 (Junction Table) 的 ID 集合
      List<Long> damageIds,
      List<Long> hintIds,
      List<Long> pestCategoryIds,
      List<@Valid PestCategoryNote> pestCategoryWithNotes,
      List<Long> identifierIds) {

    public record PestCategoryNote (@NotNull (message = "病蟲害分類不可為空") Long pestCategoryId,
        @jakarta.validation.constraints.Size (max = 500, message = "害物備註不可超過 500 字元") String pestNote) {
    }
  }

  /** 案件更新請求 (可局部更新) */
  public record CaseUpdateRequest (LocalDate receiveDate,
      String cropScale,
      String damageScale,
      String caseDescription,
      String hintDescription,
      String status,
      Long methodId,
      Long cropId,
      Long serviceId,
      Long deliverId,

      Long fieldDistrictId,

      // 送件人 (Sender) 欄位：任一提供即更新案件關聯的送件人
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
      List<@Valid PestCategoryNote> pestCategoryWithNotes,
      List<Long> identifierIds) {

    // 相容舊版 19 參數建構 (測試仍使用)
    public CaseUpdateRequest (LocalDate receiveDate, String cropScale, String damageScale,
        String caseDescription, String hintDescription, String status,
        Long methodId, Long cropId, Long serviceId, Long deliverId,
        String senderName, String senderPhone, String senderAddress,
        Long senderDistrictId, Long senderTypeId,
        List<Long> damageIds, List<Long> hintIds, List<Long> pestCategoryIds, List<Long> identifierIds) {
      this (receiveDate, cropScale, damageScale, caseDescription, hintDescription, status,
          methodId, cropId, serviceId, deliverId, null,
          null, senderName, null, senderPhone, senderAddress, senderDistrictId, senderTypeId,
          damageIds, hintIds, pestCategoryIds, null, identifierIds);
    }

    // 相容 20 參數 (有 senderId/displayName，測試仍使用)
    public CaseUpdateRequest (LocalDate receiveDate, String cropScale, String damageScale,
        String caseDescription, String hintDescription, String status,
        Long methodId, Long cropId, Long serviceId, Long deliverId,
        Long senderId, String senderName, String senderDisplayName, String senderPhone, String senderAddress,
        Long senderDistrictId, Long senderTypeId,
        List<Long> damageIds, List<Long> hintIds, List<Long> pestCategoryIds, List<Long> identifierIds) {
      this (receiveDate, cropScale, damageScale, caseDescription, hintDescription, status,
          methodId, cropId, serviceId, deliverId, null,
          senderId, senderName, senderDisplayName, senderPhone, senderAddress, senderDistrictId, senderTypeId,
          damageIds, hintIds, pestCategoryIds, null, identifierIds);
    }

    // 相容 22 參數 (無 fieldDistrictId，含 pestCategoryWithNotes，測試仍使用)
    public CaseUpdateRequest (LocalDate receiveDate, String cropScale, String damageScale,
        String caseDescription, String hintDescription, String status,
        Long methodId, Long cropId, Long serviceId, Long deliverId,
        Long senderId, String senderName, String senderDisplayName, String senderPhone, String senderAddress,
        Long senderDistrictId, Long senderTypeId,
        List<Long> damageIds, List<Long> hintIds, List<Long> pestCategoryIds,
        List<PestCategoryNote> pestCategoryWithNotes, List<Long> identifierIds) {
      this (receiveDate, cropScale, damageScale, caseDescription, hintDescription, status,
          methodId, cropId, serviceId, deliverId, null,
          senderId, senderName, senderDisplayName, senderPhone, senderAddress, senderDistrictId, senderTypeId,
          damageIds, hintIds, pestCategoryIds, pestCategoryWithNotes, identifierIds);
    }

    public record PestCategoryNote (@NotNull (message = "病蟲害分類不可為空") Long pestCategoryId,
        @jakarta.validation.constraints.Size (max = 500, message = "害物備註不可超過 500 字元") String pestNote) {
    }
  }

  /** 案件列表篩選條件 (查詢參數，皆可空，視圖 `v_case_search` 多欄) */
  public record CaseFilter (Long cropId,
      Long serviceId,
      String senderName,
      String senderQuery,
      LocalDate receiveDateFrom,
      LocalDate receiveDateTo,
      String status,
      Long cityId,
      Long districtId,
      Long cropCategoryId,
      Long pestTypeId,
      Long pestCategoryId,
      Long hintId,
      Long deliveryId,
      Long damageId,
      Long senderTypeId,
      Long methodId) {

    /** 空篩選 (等同不分條件) */
    public static CaseFilter empty () {
      return new CaseFilter (null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** 是否有任一條件 */
    public boolean isEmpty () {
      return cropId == null && serviceId == null && senderName == null && senderQuery == null
          && receiveDateFrom == null && receiveDateTo == null && status == null
          && cityId == null && districtId == null && cropCategoryId == null
          && pestTypeId == null && pestCategoryId == null && hintId == null && deliveryId == null
          && damageId == null && senderTypeId == null && methodId == null;
    }

    /** 相容舊 6 欄建構 */
    public CaseFilter (Long cropId, Long serviceId, String senderName,
        LocalDate receiveDateFrom, LocalDate receiveDateTo, String status) {
      this (cropId, serviceId, senderName, senderName, receiveDateFrom, receiveDateTo, status,
          null, null, null, null, null, null, null, null, null, null);
    }
  }

  /** 案件列表 (摘要) 回應：不帶多對多關聯，節省查詢量；pestCategoryCount>1 為複合案件，pestType 為害物類型 */
  public record CaseSummaryResponse (Long caseId,
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
      String deliveryName,
      String status,
      @JsonFormat (pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
      Integer pestCategoryCount,
      String pestCategoryNames,
      String pestTypeNames) {

    // 相容 14 欄建構 (無複合計數，ControllerTest 仍使用)
    public CaseSummaryResponse (Long caseId, LocalDate receiveDate, String cropName,
        String senderName, String senderDisplayName, String senderPhone, String senderAddress,
        Long senderId, Long senderDistrictId, String senderDistrictName, String senderCityName,
        String serviceName, String status, LocalDateTime createdAt) {
      this (caseId, receiveDate, cropName, senderName, senderDisplayName, senderPhone, senderAddress,
          senderId, senderDistrictId, senderDistrictName, senderCityName, serviceName, null, status, createdAt, 0, null, null);
    }
  }

  /** 案件詳細回應 */
  public record CaseResponse (Long caseId,
      LocalDate receiveDate,
      String cropScale,
      String damageScale,
      String caseDescription,
      String hintDescription,
      String status,
      @JsonFormat (pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
      @JsonFormat (pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt,
      Long senderId,
      String senderName,
      String senderDisplayName,
      String senderPhone,
      String senderAddress,
      Long senderDistrictId,
      String senderDistrictName,
      String senderCityName,
      Long senderTypeId,
      String senderTypeName,
      Long fieldDistrictId,
      String fieldDistrictName,
      String fieldCityName,
      String cropCategoryName,
      String cropName,
      String methodName,
      String serviceName,
      String deliveryName,
      String createdByName,
      List<IdName> damages,
      List<IdName> hints,
      List<IdNameWithNote> pestCategories,
      List<IdName> identifiers) {

    /** 通用的「ID + 名稱」結構，用於多對多關聯 */
    public record IdName (Long id, String name) {
    }

    /** 病蟲害分類含備註 (學名：描述) */
    public record IdNameWithNote (Long id, String name, String pestNote, Long pestTypeId, String pestTypeName) {
      public IdNameWithNote (Long id, String name, String pestNote) {
        this (id, name, pestNote, null, null);
      }
    }
  }
}
