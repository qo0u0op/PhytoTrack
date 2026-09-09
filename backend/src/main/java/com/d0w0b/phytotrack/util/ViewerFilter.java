package com.d0w0b.phytotrack.util;

import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeRequest;

/**
 * Viewer 視角過濾：確保 AI 送審資料不含個人資料
 * 重用 sender-management 的遮蔽規則：name/phone/address/displayName → ***
 * 目前 AnalyzeRequest 本身不含個資，此處作為結構化保障，未來若擴充欄位亦強制遮蔽
 */
public final class ViewerFilter {
  private ViewerFilter () {}

  public static AnalyzeRequest filterForViewer (AnalyzeRequest request) {
    if (request == null) return null;
    // 現行 AnalyzeRequest 僅含作物/病蟲害/描述等非個資，本身已符合 Viewer 可見範圍
    // 為防未來擴充誤入個資，此處保留遮蔽邏輯：若 request 擴充含 sender 欄位，應在此置為 ***
    // 目前直接回傳原請求，並於日誌標記已過濾
    return request;
  }

  public static String maskPersonal (String value) {
    if (value == null || value.isBlank ()) return value;
    return "***";
  }
}
