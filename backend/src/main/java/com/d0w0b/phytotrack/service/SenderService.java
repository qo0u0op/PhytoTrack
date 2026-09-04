package com.d0w0b.phytotrack.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.SenderDtos.SenderResponse;
import com.d0w0b.phytotrack.dto.SenderDtos.SenderUpsertRequest;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Sender;
import com.d0w0b.phytotrack.repository.CaseRepository;
import com.d0w0b.phytotrack.repository.DistrictRepository;
import com.d0w0b.phytotrack.repository.SenderRepository;
import com.d0w0b.phytotrack.repository.SenderTypeRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 送件人服務 (Sender Service)
 *
 * 提供搜尋、建立、編輯與硬刪除，供建案去重候選與管理頁使用。
 * STAFF+ 可建立與編輯；刪除僅 ADMIN (於 Controller 限制)。
 */
@Service
public class SenderService {

  private final SenderRepository senderRepository;
  private final CaseRepository caseRepository;
  private final DistrictRepository districtRepository;
  private final SenderTypeRepository senderTypeRepository;

  public SenderService (SenderRepository senderRepository,
                       CaseRepository caseRepository,
                       DistrictRepository districtRepository,
                       SenderTypeRepository senderTypeRepository) {
    this.senderRepository = senderRepository;
    this.caseRepository = caseRepository;
    this.districtRepository = districtRepository;
    this.senderTypeRepository = senderTypeRepository;
  }

  @Transactional (readOnly = true)
  public List<SenderResponse> search (String q) {
    if (q == null || q.isBlank ()) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "搜尋關鍵字不可為空白");
    }
    String trimmed = q.trim ();
    return senderRepository.search (trimmed).stream ()
        .limit (10)
        .map (this::toResponse)
        .toList ();
  }

  @Transactional (readOnly = true)
  public List<SenderResponse> list () {
    return senderRepository.findAll ().stream ().map (this::toResponse).toList ();
  }

  @Transactional (readOnly = true)
  public SenderResponse detail (Long id) {
    Sender s = findOrThrow (id);
    return toResponse (s);
  }

  /** 建立 (STAFF+)：phone 與 displayName 至少一有值 */
  @Transactional
  public SenderResponse create (SenderUpsertRequest request) {
    validateContact (request);
    Sender s = new Sender ();
    apply (s, request);
    return toResponse (senderRepository.save (s));
  }

  /** 編輯 (STAFF+)：phone 與 displayName 至少一有值 */
  @Transactional
  public SenderResponse update (Long id, SenderUpsertRequest request) {
    Sender s = findOrThrow (id);
    validateContact (request);
    apply (s, request);
    return toResponse (senderRepository.save (s));
  }

  /** 硬刪除 (僅 ADMIN，被引用時 409) */
  @Transactional
  public void delete (Long id) {
    Sender sender = findOrThrow (id);
    if (caseRepository.existsBySenderSenderId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    senderRepository.delete (sender);
  }

  private Sender findOrThrow (Long id) {
    return senderRepository.findById (id)
        .orElseThrow (() -> new ApiException ("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
  }

  /** phone 與 displayName 至少一有值 (trim 後判斷) */
  private void validateContact (SenderUpsertRequest request) {
    boolean hasPhone = request.phone () != null && !request.phone ().isBlank ();
    boolean hasDisplay = request.displayName () != null && !request.displayName ().isBlank ();
    if (!hasPhone && !hasDisplay) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "電話與顯示名稱至少需提供一項");
    }
    InputSanitizer.assertNoHtml (request.displayName (), "顯示名稱");
    InputSanitizer.assertNoHtml (request.name (), "姓名");
    InputSanitizer.assertNoHtml (request.address (), "地址");
  }

  private void apply (Sender s, SenderUpsertRequest request) {
    s.setName (blankToNull (request.name ()));
    s.setDisplayName (blankToNull (request.displayName ()));
    s.setPhone (blankToNull (request.phone ()));
    s.setAddress (blankToNull (request.address ()));
    s.setDistrict (getRef (districtRepository, request.districtId (), "鄉鎮市區"));
    s.setSenderType (getRef (senderTypeRepository, request.senderTypeId (), "身分別"));
  }

  private static String blankToNull (String v) {
    return v == null || v.isBlank () ? null : v.trim ();
  }

  private <T> T getRef (org.springframework.data.jpa.repository.JpaRepository<T, Long> repo, Long id, String label) {
    return repo.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, label + "不存在"));
  }

  private SenderResponse toResponse (Sender s) {
    boolean isViewer = isViewer ();
    String phone = isViewer ? null : s.getPhone ();
    String address = isViewer ? null : s.getAddress ();
    String districtName = null;
    String cityName = null;
    Long districtId = null;
    if (s.getDistrict () != null) {
      districtName = s.getDistrict ().getDistrict ();
      districtId = s.getDistrict ().getDistrictId ();
      if (s.getDistrict ().getCity () != null) {
        cityName = s.getDistrict ().getCity ().getCity ();
      }
    }
    Long senderTypeId = null;
    String senderTypeName = null;
    if (s.getSenderType () != null) {
      senderTypeId = s.getSenderType ().getSenderTypeId ();
      senderTypeName = s.getSenderType ().getSenderType ();
    }
    // VIEWER 時姓名與顯示名稱亦視為個資，一併遮蔽 (僅留縣市鄉鎮)
    String name = isViewer ? null : s.getName ();
    String displayName = isViewer ? null : s.getDisplayName ();
    return new SenderResponse (s.getSenderId (),
        name,
        displayName,
        phone,
        address,
        districtId,
        districtName,
        cityName,
        senderTypeId,
        senderTypeName);
  }

  private boolean isViewer () {
    Authentication auth = SecurityContextHolder.getContext ().getAuthentication ();
    return auth != null && auth.getAuthorities ().stream ()
        .anyMatch (a -> "ROLE_VIEWER".equals (a.getAuthority ()));
  }
}
