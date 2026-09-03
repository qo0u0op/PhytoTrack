package com.d0w0b.phytotrack.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.IdentifierRepository;

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

  public IdentifierService (IdentifierRepository identifierRepository) {
    this.identifierRepository = identifierRepository;
  }

  @Transactional
  public Identifier ensureForUser (User user) {
    if (user == null || user.getUserId () == null) {
      throw new IllegalArgumentException ("user 不可為空");
    }
    List<Identifier> existing = identifierRepository.findByUserUserId (user.getUserId ());
    if (!existing.isEmpty ()) {
      Identifier first = existing.get (0);
      String displayName = user.getDisplayName ();
      if (displayName != null && !displayName.equals (first.getIdentifier ())) {
        first.setIdentifier (displayName.trim ());
        // 若有多筆，僅同步首筆，其餘保留（相容既有 staff 2 筆種子）
      }
      return first;
    }
    Identifier identifier = new Identifier ();
    identifier.setIdentifier (user.getDisplayName () != null ? user.getDisplayName ().trim () : user.getUsername ());
    identifier.setUser (user);
    return identifierRepository.save (identifier);
  }

  @Transactional (readOnly = true)
  public List<Identifier> findByUserId (Long userId) {
    return identifierRepository.findByUserUserId (userId);
  }
}
