package com.d0w0b.phytotrack.service;

import com.d0w0b.phytotrack.dto.AccountDtos.DeactivateRequestResponse;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.DeactivateRequest;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.DeactivateRequestRepository;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.UserRepository;
import com.d0w0b.phytotrack.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AccountService {

  private final UserRepository userRepository;
  private final DeactivateRequestRepository deactivateRequestRepository;
  private final PasswordEncoder passwordEncoder;
  private final IdentifierRepository identifierRepository;
  private final IdentifierService identifierService;

  @org.springframework.beans.factory.annotation.Autowired
  public AccountService (UserRepository userRepository,
                         DeactivateRequestRepository deactivateRequestRepository,
                         PasswordEncoder passwordEncoder,
                         @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierRepository identifierRepository,
                         @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierService identifierService) {
    this.userRepository = userRepository;
    this.deactivateRequestRepository = deactivateRequestRepository;
    this.passwordEncoder = passwordEncoder;
    this.identifierRepository = identifierRepository;
    this.identifierService = identifierService;
  }

  // 相容舊呼叫（測試或尚未更新處）
  public AccountService (UserRepository userRepository,
                         DeactivateRequestRepository deactivateRequestRepository,
                         PasswordEncoder passwordEncoder) {
    this (userRepository, deactivateRequestRepository, passwordEncoder, null, null);
  }

  @Transactional(readOnly = true)
  public UserResponse getProfile (UserPrincipal principal) {
    User user = userRepository.findById (principal.getUserId ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    return toResponse (user);
  }

  @Transactional
  public UserResponse updateProfile (UserPrincipal principal, String displayName, String email) {
    User user = userRepository.findById (principal.getUserId ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    String trimmedEmail = email != null ? email.trim() : null;
    if (trimmedEmail != null && trimmedEmail.isEmpty ()) trimmedEmail = null;

    user.setDisplayName (displayName.trim ());
    user.setEmail (trimmedEmail);
    userRepository.save (user);
    // 同步簽名人：displayName 變更時更名首個 Identifier，無則建立
    syncIdentifierForDisplayName (user);
    return toResponse (user);
  }

  private void syncIdentifierForDisplayName (User user) {
    if (identifierRepository == null) return;
    String displayName = user.getDisplayName ();
    if (displayName == null || displayName.isBlank ()) return;
    // VIEWER 不強制，但有則同步更名
    List<Identifier> existing = identifierRepository.findByUserUserId (user.getUserId ());
    if (!existing.isEmpty ()) {
      Identifier first = existing.get (0);
      if (!displayName.trim ().equals (first.getIdentifier ())) {
        first.setIdentifier (displayName.trim ());
      }
    } else if ((user.getRole () == User.Role.ROLE_STAFF || user.getRole () == User.Role.ROLE_ADMIN)
        && identifierService != null) {
      identifierService.ensureForUser (user);
    }
  }

  @Transactional(readOnly = true)
  public boolean isEmailAvailable (String email, Long currentUserId) {
    if (email == null || email.trim ().isEmpty ()) return true;
    String trimmed = email.trim ();
    var existing = userRepository.findByEmailIgnoreCase (trimmed);
    if (existing.isEmpty ()) return true;
    return existing.get ().getUserId ().equals (currentUserId);
  }

  @Transactional
  public void changePassword (UserPrincipal principal, String currentPassword, String newPassword) {
    User user = userRepository.findById (principal.getUserId ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    boolean isAdmin = principal.getAuthorities ().stream ().anyMatch (a -> a.getAuthority ().equals ("ROLE_ADMIN"));
    if (!isAdmin) {
      if (currentPassword == null || currentPassword.isBlank ()) {
        throw new ApiException ("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED, "目前密碼不可為空");
      }
      if (!passwordEncoder.matches (currentPassword, user.getPassword ())) {
        throw new ApiException ("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED, "目前密碼不正確");
      }
    } else {
      // ADMIN 修改自身密碼仍需現密碼，非 ADMIN 情況已處理；ADMIN 為他人重設走舊端點
      if (currentPassword != null && !currentPassword.isBlank ()) {
        if (!passwordEncoder.matches (currentPassword, user.getPassword ())) {
          throw new ApiException ("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED, "目前密碼不正確");
        }
      }
    }
    user.setPassword (passwordEncoder.encode (newPassword));
    userRepository.save (user);
  }

  @Transactional
  public DeactivateRequestResponse requestDeactivate (UserPrincipal principal) {
    Long userId = principal.getUserId ();
    User user = userRepository.findById (userId)
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    // 管理員僅能經使用者管理停用，不提供自助停用申請
    if (user.getRole () == User.Role.ROLE_ADMIN) {
      throw new ApiException ("ADMIN_DEACTIVATE_FORBIDDEN", HttpStatus.FORBIDDEN, "管理員請經使用者管理停用");
    }
    if (deactivateRequestRepository.existsByUserUserIdAndStatus (userId, DeactivateRequest.Status.PENDING)) {
      throw new ApiException ("DEACTIVATE_PENDING", HttpStatus.CONFLICT, "已有待審核的停用請求");
    }
    if (!user.isActive ()) {
      throw new ApiException ("ALREADY_INACTIVE", HttpStatus.CONFLICT, "帳號已停用");
    }
    DeactivateRequest req = new DeactivateRequest ();
    req.setUser (user);
    req.setStatus (DeactivateRequest.Status.PENDING);
    deactivateRequestRepository.save (req);
    return toDeactivateResponse (req);
  }

  @Transactional(readOnly = true)
  public DeactivateRequestResponse getMyPending (UserPrincipal principal) {
    return deactivateRequestRepository.findByUserUserIdAndStatus (principal.getUserId (), DeactivateRequest.Status.PENDING)
        .map (this::toDeactivateResponse).orElse (null);
  }

  @Transactional
  public void cancelDeactivate (UserPrincipal principal) {
    var opt = deactivateRequestRepository.findByUserUserIdAndStatus (principal.getUserId (), DeactivateRequest.Status.PENDING);
    if (opt.isEmpty ()) {
      throw new ApiException ("REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "無待審核請求");
    }
    DeactivateRequest req = opt.get ();
    req.setStatus (DeactivateRequest.Status.REJECTED);
    req.setReviewedBy (req.getUser ());
    req.setReviewedAt (java.time.LocalDateTime.now ());
    deactivateRequestRepository.save (req);
  }

  @Transactional
  public void abandonDeactivateByUsername (String username, String password) {
    User user = userRepository.findByUsername (username)
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    if (!passwordEncoder.matches (password, user.getPassword ())) {
      throw new ApiException ("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED, "密碼不正確");
    }
    var opt = deactivateRequestRepository.findByUserUserIdAndStatus (user.getUserId (), DeactivateRequest.Status.PENDING);
    if (opt.isEmpty ()) {
      throw new ApiException ("REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "無待審核請求");
    }
    DeactivateRequest req = opt.get ();
    req.setStatus (DeactivateRequest.Status.REJECTED);
    req.setReviewedBy (user);
    req.setReviewedAt (java.time.LocalDateTime.now ());
    deactivateRequestRepository.save (req);
  }

  @Transactional(readOnly = true)
  public List<DeactivateRequestResponse> listRequests () {
    return deactivateRequestRepository.findAllByOrderByCreatedAtDesc ().stream ()
        .map (this::toDeactivateResponse).toList ();
  }

  @Transactional
  public DeactivateRequestResponse reviewRequest (Long requestId, String status, UserPrincipal reviewer) {
    DeactivateRequest req = deactivateRequestRepository.findById (requestId)
        .orElseThrow (() -> new ApiException ("REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "請求不存在"));
    if (req.getStatus () != DeactivateRequest.Status.PENDING) {
      throw new ApiException ("REQUEST_NOT_PENDING", HttpStatus.CONFLICT, "請求已處理");
    }
    DeactivateRequest.Status newStatus;
    try {
      newStatus = DeactivateRequest.Status.valueOf (status);
    } catch (Exception e) {
      throw new ApiException ("INVALID_STATUS", HttpStatus.BAD_REQUEST, "狀態不正確");
    }
    if (newStatus == DeactivateRequest.Status.PENDING) {
      throw new ApiException ("INVALID_STATUS", HttpStatus.BAD_REQUEST, "狀態不正確");
    }
    // 不可自審通過停用
    if (newStatus == DeactivateRequest.Status.APPROVED && reviewer.getUserId ().equals (req.getUser ().getUserId ())) {
      throw new ApiException ("SELF_DISABLE_FORBIDDEN", HttpStatus.FORBIDDEN, "不可審核自己的停用請求");
    }
    User reviewerUser = userRepository.findById (reviewer.getUserId ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    req.setStatus (newStatus);
    req.setReviewedBy (reviewerUser);
    req.setReviewedAt (java.time.LocalDateTime.now ());
    if (newStatus == DeactivateRequest.Status.APPROVED) {
      User target = req.getUser ();
      // 不可停用最後一位 ADMIN
      if (target.getRole () == User.Role.ROLE_ADMIN && target.isActive ()) {
        if (userRepository.countByRoleAndActive (User.Role.ROLE_ADMIN, true) <= 1) {
          throw new ApiException ("LAST_ADMIN_FORBIDDEN", HttpStatus.CONFLICT, "不可停用最後一位管理者");
        }
      }
      target.setActive (false);
      userRepository.save (target);
    }
    deactivateRequestRepository.save (req);
    return toDeactivateResponse (req);
  }

  private UserResponse toResponse (User user) {
    return new UserResponse (user.getUserId (), user.getUsername (), user.getDisplayName (), user.getEmail (), user.getRole ().name (), user.isActive ());
  }

  private DeactivateRequestResponse toDeactivateResponse (DeactivateRequest r) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm:ss");
    String created = r.getCreatedAt () != null ? r.getCreatedAt ().format (fmt) : null;
    String reviewer = r.getReviewedBy () != null ? r.getReviewedBy ().getUsername () : null;
    return new DeactivateRequestResponse (r.getRequestId (), r.getUser ().getUserId (), r.getUser ().getUsername (), r.getStatus ().name (), created, reviewer);
  }
}
