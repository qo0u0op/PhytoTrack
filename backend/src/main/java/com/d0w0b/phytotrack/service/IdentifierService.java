package com.d0w0b.phytotrack.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.UserRepository;

import java.util.List;

/**
 * 診斷簽名人服務（IdentifierService）
 *
 * 集中管理 Identifier 與 User.displayName 的同步：
 * - ensureForUser(User)：無則建立、有則在 displayName 變更時更名
 */
@Service
public class IdentifierService {

  private final IdentifierRepository identifierRepository;
  private final UserRepository userRepository;

  public IdentifierService (IdentifierRepository identifierRepository, UserRepository userRepository) {
    this.identifierRepository = identifierRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Identifier ensureForUser (User user) {
    if (user == null || user.getUserId () == null) {
      throw new IllegalArgumentException ("user 不可為空");
    }
    List<Identifier> active = identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (user.getUserId ());
    if (!active.isEmpty ()) {
      Identifier first = active.get (0);
      String displayName = user.getDisplayName ();
      if (displayName != null && !IdentifierNames.equalsNormalized (first.getIdentifier (), displayName)) {
        first.setIdentifier (IdentifierNames.display (displayName));
      }
      return first;
    }
    // 無 active（可能僅有已停用或全新），建新 active 前先查全域撞名；
    // 若有同名 inactive 舊筆則優先啟用，避免累積
    String newName = IdentifierNames.display (user.getDisplayName () != null ? user.getDisplayName () : user.getUsername ());
    synchronized (("signer:" + IdentifierNames.normalize (newName)).intern ()) {
      // 僅擋非使用者同名（同名同姓的使用者簽名人允許並存，以帳號區分）
      boolean collides = identifierRepository.findByUserIsNullAndActiveTrue ().stream ()
          .anyMatch (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), newName));
      if (collides) {
        throw new com.d0w0b.phytotrack.exception.ApiException ("DISPLAY_NAME_EXISTS", org.springframework.http.HttpStatus.CONFLICT, "顯示名稱已存在");
      }
      List<Identifier> inactiveSameName = identifierRepository.findByUserUserId (user.getUserId ()).stream ()
          .filter (i -> !i.isActive () && IdentifierNames.equalsNormalized (i.getIdentifier (), newName))
          .sorted (java.util.Comparator.comparing (Identifier::getIdentifierId))
          .toList ();
      if (!inactiveSameName.isEmpty ()) {
        Identifier reactivated = inactiveSameName.get (0);
        reactivated.setActive (true);
        return identifierRepository.save (reactivated);
      }
      Identifier identifier = new Identifier ();
      identifier.setIdentifier (newName);
      identifier.setUser (user);
      identifier.setActive (true);
      try {
        return identifierRepository.save (identifier);
      } catch (org.springframework.dao.DataIntegrityViolationException ex) {
        throw new com.d0w0b.phytotrack.exception.ApiException ("DISPLAY_NAME_EXISTS", org.springframework.http.HttpStatus.CONFLICT, "顯示名稱已存在");
      }
    }
  }

  @Transactional
  public Identifier updateActive (Long id, boolean active) {
    Identifier e = identifierRepository.findById (id)
        .orElseThrow (() -> new com.d0w0b.phytotrack.exception.ApiException ("REFERENCE_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND, "簽名人不存在"));
    e.setActive (active);
    return e;
  }

  @Transactional (readOnly = true)
  public List<Identifier> findByUserId (Long userId) {
    return identifierRepository.findByUserUserId (userId);
  }

  @Transactional
  public Identifier bindToUser (Long identifierId, Long userId) {
    Identifier identifier = identifierRepository.findById (identifierId)
        .orElseThrow (() -> new com.d0w0b.phytotrack.exception.ApiException ("REFERENCE_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND, "簽名人不存在"));
    if (identifier.getUser () != null) {
      throw new com.d0w0b.phytotrack.exception.ApiException ("ALREADY_BOUND", org.springframework.http.HttpStatus.CONFLICT, "簽名人已綁定使用者");
    }
    User user = userRepository.findById (userId)
        .orElseThrow (() -> new com.d0w0b.phytotrack.exception.ApiException ("USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND, "使用者不存在"));
    // 名實一致：來源簽名人名稱須與目標使用者 displayName 一致（正規化後），否則拒絕
    String userName = user.getDisplayName () != null ? user.getDisplayName () : user.getUsername ();
    if (!IdentifierNames.equalsNormalized (identifier.getIdentifier (), userName)) {
      throw new com.d0w0b.phytotrack.exception.ApiException ("SIGNER_NAME_MISMATCH", org.springframework.http.HttpStatus.BAD_REQUEST, "簽名人名稱與使用者顯示名稱不一致，請先改名");
    }
    // 若使用者已有同名 active 簽名人，提示衝突
    boolean hasSameName = identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (userId).stream ()
        .anyMatch (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), identifier.getIdentifier ()));
    if (hasSameName) {
      throw new com.d0w0b.phytotrack.exception.ApiException ("ALREADY_BOUND", org.springframework.http.HttpStatus.CONFLICT, "使用者已有同名簽名人");
    }
    identifier.setUser (user);
    identifier.setActive (true);
    return identifierRepository.save (identifier);
  }
}
