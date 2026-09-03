package com.d0w0b.phytotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 參照資料 (Reference Data) 回應 DTO
 *
 * 參照資料是診斷表單的下拉選單來源 (作物、病蟲害類別、被害部位等)，
 * 為唯讀資料，由 ReferenceDataController 提供。
 */
public final class ReferenceDtos {

  private ReferenceDtos () {
  }

  /** 作物分類回應：包含其下的作物清單 */
  public record CropCategoryResponse (Long id, String name, List<CropItem> crops) {
    public record CropItem (Long id, String name) {
    }
  }

  /** 害物類型回應：包含其下的病蟲害小分類清單 */
  public record PestTypeResponse (Long id, String name, List<PestCategoryItem> categories) {
    public record PestCategoryItem (Long id, String code, String name) {
    }
  }

  /** 縣市回應：包含其下的鄉鎮市區清單 */
  public record CityResponse (Long id, String name, List<DistrictItem> districts) {
    public record DistrictItem (Long id, String name, int sortOrder) {
    }
  }

  /** 通用「ID + 名稱」回應 (被害部位、防治建議、耕種方式、送件方式、服務類別、身分別等) */
  public record IdNameResponse (Long id, String name) {
  }

  public record IdentifierResponse (Long id, String name, boolean active, Long userId, String username) {
  }

  /** 通用名稱寫入請求 (用於 damages/hints/methods/deliveries/services/identifiers/sender-types) */
  public record IdNameCreateRequest (@NotBlank (message = "名稱不可為空白")
      @Size (max = 100, message = "名稱不可超過 100 字元")
      String name) {
  }

  public record IdNameUpdateRequest (@NotBlank (message = "名稱不可為空白")
      @Size (max = 100, message = "名稱不可超過 100 字元")
      String name) {
  }

  /** 作物寫入請求 */
  public record CropCreateRequest (@NotBlank (message = "作物名稱不可為空白")
      @Size (max = 100, message = "作物名稱不可超過 100 字元")
      String name,
      @NotNull (message = "作物分類不可為空")
      Long cropCategoryId) {
  }

  public record CropUpdateRequest (@NotBlank (message = "作物名稱不可為空白")
      @Size (max = 100, message = "作物名稱不可超過 100 字元")
      String name,
      @NotNull (message = "作物分類不可為空")
      Long cropCategoryId) {
  }

  /** 作物分類寫入請求 */
  public record CropCategoryCreateRequest (@NotBlank (message = "分類名稱不可為空白")
      @Size (max = 100, message = "分類名稱不可超過 100 字元")
      String name) {
  }

  public record CropCategoryUpdateRequest (@NotBlank (message = "分類名稱不可為空白")
      @Size (max = 100, message = "分類名稱不可超過 100 字元")
      String name) {
  }

  /** 病蟲害小分類寫入請求 */
  public record PestCategoryCreateRequest (@NotBlank (message = "代碼不可為空白")
      @Size (max = 20, message = "代碼不可超過 20 字元")
      String code,
      @NotBlank (message = "名稱不可為空白")
      @Size (max = 100, message = "名稱不可超過 100 字元")
      String name,
      @NotNull (message = "害物類型不可為空")
      Long pestTypeId) {
  }

  public record PestCategoryUpdateRequest (@NotBlank (message = "代碼不可為空白")
      @Size (max = 20, message = "代碼不可超過 20 字元")
      String code,
      @NotBlank (message = "名稱不可為空白")
      @Size (max = 100, message = "名稱不可超過 100 字元")
      String name,
      @NotNull (message = "害物類型不可為空")
      Long pestTypeId) {
  }

  public record ActiveUpdateRequest (@NotNull (message = "啟用狀態不可為空")
      Boolean active) {
  }

  public record BindSignerRequest (@NotNull (message = "使用者不可為空")
      Long userId) {
  }
}