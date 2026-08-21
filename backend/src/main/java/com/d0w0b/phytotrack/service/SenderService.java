package com.d0w0b.phytotrack.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.SenderDtos.SenderResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Sender;
import com.d0w0b.phytotrack.repository.CaseRepository;
import com.d0w0b.phytotrack.repository.SenderRepository;

import java.util.List;

/**
 * 送件人服務（Sender Service）
 *
 * 提供搜尋與硬刪除，供建案去重候選與管理頁使用。
 */
@Service
public class SenderService {

  private final SenderRepository senderRepository;
  private final CaseRepository caseRepository;

  public SenderService(SenderRepository senderRepository, CaseRepository caseRepository) {
    this.senderRepository = senderRepository;
    this.caseRepository = caseRepository;
  }

  @Transactional(readOnly = true)
  public List<SenderResponse> search(String q) {
    if (q == null || q.isBlank()) {
      throw new ApiException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "搜尋關鍵字不可為空白");
    }
    String trimmed = q.trim();
    return senderRepository.search(trimmed).stream()
        .limit(10)
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SenderResponse> list() {
    return senderRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public SenderResponse detail(Long id) {
    Sender s = senderRepository.findById(id)
        .orElseThrow(() -> new ApiException("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
    return toResponse(s);
  }

  @Transactional
  public void delete(Long id) {
    Sender sender = senderRepository.findById(id)
        .orElseThrow(() -> new ApiException("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
    if (caseRepository.existsBySenderSenderId(id)) {
      throw new ApiException("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    senderRepository.delete(sender);
  }

  private SenderResponse toResponse(Sender s) {
    String districtName = null;
    String cityName = null;
    Long districtId = null;
    if (s.getDistrict() != null) {
      districtName = s.getDistrict().getDistrict();
      districtId = s.getDistrict().getDistrictId();
      if (s.getDistrict().getCity() != null) {
        cityName = s.getDistrict().getCity().getCity();
      }
    }
    Long senderTypeId = null;
    String senderTypeName = null;
    if (s.getSenderType() != null) {
      senderTypeId = s.getSenderType().getSenderTypeId();
      senderTypeName = s.getSenderType().getSenderType();
    }
    return new SenderResponse(
        s.getSenderId(),
        s.getName(),
        s.getDisplayName(),
        s.getPhone(),
        s.getAddress(),
        districtId,
        districtName,
        cityName,
        senderTypeId,
        senderTypeName);
  }
}
