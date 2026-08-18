package com.d0w0b.phytotrack.dto;

import java.util.List;

/**
 * 參照資料（Reference Data）回應 DTO
 *
 * 參照資料是診斷表單的下拉選單來源（作物、病蟲害類別、被害部位等），
 * 為唯讀資料，由 ReferenceDataController 提供。
 */
public final class ReferenceDtos {

  private ReferenceDtos() {
  }

  /** 作物分類回應：包含其下的作物清單 */
  public record CropCategoryResponse(Long id, String name, List<CropItem> crops) {
    public record CropItem(Long id, String name) {
    }
  }

  /** 害物類型回應：包含其下的病蟲害小分類清單 */
  public record PestTypeResponse(Long id, String name, List<PestCategoryItem> categories) {
    public record PestCategoryItem(Long id, String code, String name, int sortOrder) {
    }
  }

  /** 縣市回應：包含其下的鄉鎮市區清單 */
  public record CityResponse(Long id, String name, List<DistrictItem> districts) {
    public record DistrictItem(Long id, String name, int sortOrder) {
    }
  }

  /** 通用「ID + 名稱」回應（被害部位、防治建議、耕種方式、送件方式、服務類別、身分別等） */
  public record IdNameResponse(Long id, String name) {
  }
}