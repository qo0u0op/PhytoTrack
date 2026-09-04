package com.d0w0b.phytotrack.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.AuthDtos.AuthResponse;
import com.d0w0b.phytotrack.dto.AuthDtos.LoginRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.RegisterRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.DeactivateRequest;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.DeactivateRequestRepository;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.UserRepository;
import com.d0w0b.phytotrack.security.JwtTokenProvider;
import com.d0w0b.phytotrack.security.UserPrincipal;

import java.util.List;

/**
 * 認證服務 (Authentication Service)
 *
 * 職責：
 *   - 註冊：檢查唯一性、以 BCrypt 編碼密碼、預設 ROLE_VIEWER 角色
 *   - 登入：透過 AuthenticationManager 認證，成功後簽發 JWT
 *   - 查詢目前使用者
 */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final DeactivateRequestRepository deactivateRequestRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final IdentifierRepository identifierRepository;
  private final IdentifierService identifierService;

  @org.springframework.beans.factory.annotation.Autowired
  public AuthService (UserRepository userRepository,
                     DeactivateRequestRepository deactivateRequestRepository,
                     PasswordEncoder passwordEncoder,
                     AuthenticationManager authenticationManager,
                     JwtTokenProvider jwtTokenProvider,
                     @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierRepository identifierRepository,
                     @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierService identifierService) {
    this.userRepository = userRepository;
    this.deactivateRequestRepository = deactivateRequestRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtTokenProvider = jwtTokenProvider;
    this.identifierRepository = identifierRepository;
    this.identifierService = identifierService;
  }

  // 相容舊單元測試
  public AuthService (UserRepository userRepository,
                     DeactivateRequestRepository deactivateRequestRepository,
                     PasswordEncoder passwordEncoder,
                     AuthenticationManager authenticationManager,
                     JwtTokenProvider jwtTokenProvider) {
    this (userRepository, deactivateRequestRepository, passwordEncoder, authenticationManager, jwtTokenProvider, null, null);
  }

  /**
   * 註冊新使用者
   *
   * 使用 DTO (RegisterRequest) 而非 Entity 接收表單，可防止 Mass Assignment
   * 提權：攻擊者無法透過多餘欄位 (如 role) 竄改角色。
   */
  @Transactional
  public UserResponse register (RegisterRequest request) {
    if (userRepository.findByUsername (request.username ()).isPresent ()) {
      throw new ApiException ("USERNAME_TAKEN", HttpStatus.CONFLICT, "帳號已存在");
    }
    // 信箱非空時全域唯一（去空白、大小寫不敏感），與個人資料編輯一致
    String email = request.email () == null ? null : request.email ().trim ();
    if (email != null && !email.isEmpty () && userRepository.existsByEmailIgnoreCase (email)) {
      throw new ApiException ("EMAIL_TAKEN", HttpStatus.CONFLICT, "電子信箱已被使用");
    }

    User user = new User ();
    user.setUsername (request.username ());
    user.setDisplayName (request.displayName ());
    // 密碼以 BCrypt 單向雜湊儲存，絕不存明文
    user.setPassword (passwordEncoder.encode (request.password ()));
    user.setEmail (email == null || email.isEmpty () ? null : email);
    // 新使用者一律為檢視者 (Viewer)，管理員需由既有管理員調整
    user.setRole (User.Role.ROLE_VIEWER);
    user.setActive (true);

    return toResponse (userRepository.save (user));
  }

  /**
   * 帳號可用性（註冊語境）：去空白後為空視為不可用，避免空帳號被誤判可用。
   */
  @Transactional (readOnly = true)
  public boolean isUsernameAvailable (String username) {
    if (username == null || username.trim ().isEmpty ()) return false;
    return !userRepository.existsByUsername (username.trim ());
  }

  /**
   * 信箱可用性（註冊語境）：空值視為可用（選填），非空去空白、大小寫不敏感比對。
   */
  @Transactional (readOnly = true)
  public boolean isEmailAvailable (String email) {
    if (email == null || email.trim ().isEmpty ()) return true;
    return !userRepository.existsByEmailIgnoreCase (email.trim ());
  }
  /**
   * 登入：認證成功後簽發 JWT
   *
   * AuthenticationManager 會串接 CustomUserDetailsService (載入使用者) 與
   * PasswordEncoder (BCrypt 驗證)。失敗時拋出 BadCredentialsException。
   */
  @Transactional (readOnly = true)
  public AuthResponse login (LoginRequest request) {
    // 若有待審核的停用請求，先阻擋登入並提示是否放棄
    var preUser = userRepository.findByUsername (request.username ());
    if (preUser.isPresent ()) {
      boolean hasPending = deactivateRequestRepository.existsByUserUserIdAndStatus (preUser.get ().getUserId (), DeactivateRequest.Status.PENDING);
      if (hasPending) {
        throw new ApiException ("DEACTIVATE_PENDING", HttpStatus.CONFLICT, "帳號有待審核的停用申請，請選擇是否放棄");
      }
    }
    // 停用帳號由 CustomUserDetailsService 的 isEnabled ()==active 觸發 DisabledException，
    // 統一由 GlobalExceptionHandler 轉 403 ACCOUNT_DISABLED；此處不再重複檢查 (避免 dead code)
    Authentication authentication = authenticationManager.authenticate (new UsernamePasswordAuthenticationToken (request.username (), request.password ()));
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal ();

    User user = userRepository.findByUsername (principal.getUsername ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    return new AuthResponse (jwtTokenProvider.generateToken (user), toResponse (user));
  }

  /** 依目前登入者的安全主體 (Principal) 查詢使用者資訊 */
  @Transactional (readOnly = true)
  public UserResponse me (UserPrincipal principal) {
    User user = userRepository.findById (principal.getUserId ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    return toResponse (user);
  }

  /** 列出所有使用者 (限管理者使用) */
  @Transactional (readOnly = true)
  public List<UserResponse> listUsers () {
    return userRepository.findAll ().stream ()
        .map (this::toResponse)
        .toList ();
  }

  /** 管理者調整角色 */
  @Transactional
  public UserResponse updateRole (Long userId, String roleStr) {
    return updateRole (userId, new com.d0w0b.phytotrack.dto.AuthDtos.RoleUpdateRequest (roleStr));
  }

  @Transactional
  public UserResponse updateRole (Long userId, com.d0w0b.phytotrack.dto.AuthDtos.RoleUpdateRequest request) {
    String roleStr = request.role ();
    User user = userRepository.findById (userId)
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    User.Role role;
    try {
      role = User.Role.valueOf (roleStr);
    } catch (IllegalArgumentException e) {
      throw new ApiException ("INVALID_ROLE", HttpStatus.BAD_REQUEST, "角色不正確");
    }
    // 最後一位 active ADMIN 不可被降權
    if (user.getRole () == User.Role.ROLE_ADMIN && user.isActive ()
        && role != User.Role.ROLE_ADMIN) {
      if (userRepository.countByRoleAndActive (User.Role.ROLE_ADMIN, true) <= 1) {
        throw new ApiException ("LAST_ADMIN_FORBIDDEN", HttpStatus.CONFLICT, "不可移除最後一位管理者");
      }
    }
    // 若帶 bindIdentifierId，先綁定
    if (request.bindIdentifierId () != null && identifierService != null) {
      identifierService.bindToUser (request.bindIdentifierId (), userId);
    }
    // 撞名檢查：提權至 STAFF/ADMIN 且 displayName 撞 signer but not user
    boolean force = Boolean.TRUE.equals (request.force ());
    // 升權先恢復原筆（憑 former_user_id），自身舊筆恢復後不再誤報撞名
    if (!force && (role == User.Role.ROLE_STAFF || role == User.Role.ROLE_ADMIN)
        && identifierRepository != null && identifierService != null && request.bindIdentifierId () == null) {
      identifierService.restoreFormerSigner (user);
    }
    if (!force && (role == User.Role.ROLE_STAFF || role == User.Role.ROLE_ADMIN)
        && identifierRepository != null && request.bindIdentifierId () == null) {
      String displayName = user.getDisplayName ();
      if (displayName != null && !displayName.isBlank ()) {
        var existingOpt = identifierRepository.findByUserIsNullAndActiveTrue ().stream ()
            .filter (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), displayName))
            .sorted (java.util.Comparator.comparing (Identifier::getIdentifierId))
            .findFirst ();
        if (existingOpt.isPresent ()) {
          boolean userHasSame = identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (userId).stream ()
              .anyMatch (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), displayName));
          if (!userHasSame) {
            java.util.Map<String, Object> details = java.util.Map.of ("existingIdentifierId", existingOpt.get ().getIdentifierId (), "displayName", IdentifierNames.display (displayName));
            throw new ApiException ("SIGNER_NAME_CONFLICT", HttpStatus.CONFLICT, "顯示名稱與既有簽名人重名，是否綁定", details);
          }
        }
      }
    }
    User.Role oldRole = user.getRole ();
    user.setRole (role);
    User saved = userRepository.save (user);
    // 降級至 VIEWER 時解綁名下簽名人（保留 active 與 id，記 former_user_id，候選仍可見）
    if ((oldRole == User.Role.ROLE_STAFF || oldRole == User.Role.ROLE_ADMIN)
        && role == User.Role.ROLE_VIEWER
        && identifierRepository != null) {
      unlinkUserSigners (saved, false);
    }
    // 升為 STAFF/ADMIN 時確保簽名人存在（VIEWER 不強制），相容舊單測 null 情況
    if ((role == User.Role.ROLE_STAFF || role == User.Role.ROLE_ADMIN)
        && identifierRepository != null && identifierService != null) {
      // 若已透過 bind 取得，則不需再建
      boolean hasActive = !identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (saved.getUserId ()).isEmpty ();
      if (!hasActive) {
        identifierService.ensureForUser (saved);
      }
    }
    return toResponse (saved);
  }

  /** 管理者啟停用帳號 */
  @Transactional
  public UserResponse updateActive (Long userId, Boolean active) {
    if (active == null) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "啟用狀態不可為空");
    }
    User user = userRepository.findById (userId)
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    // 禁止自我停用
    Long currentUserId = currentUserId ();
    if (currentUserId != null && currentUserId.equals (userId) && !active) {
      throw new ApiException ("SELF_DISABLE_FORBIDDEN", HttpStatus.FORBIDDEN, "不可停用自己的帳號");
    }
    // 最後一位 active ADMIN 不可被停用
    if (user.getRole () == User.Role.ROLE_ADMIN && user.isActive () && !active) {
      if (userRepository.countByRoleAndActive (User.Role.ROLE_ADMIN, true) <= 1) {
        throw new ApiException ("LAST_ADMIN_FORBIDDEN", HttpStatus.CONFLICT, "不可停用最後一位管理者");
      }
    }
    user.setActive (active);
    User saved = userRepository.save (user);
    // 停用帳號時解綁並連動停用名下簽名人（歷史案件仍以 id 顯示）
    if (!active && identifierRepository != null) {
      unlinkUserSigners (saved, true);
    }
    // 重新啟用 STAFF/ADMIN 時恢復原筆簽名人
    if (active && (saved.getRole () == User.Role.ROLE_STAFF || saved.getRole () == User.Role.ROLE_ADMIN)
        && identifierRepository != null && identifierService != null) {
      identifierService.restoreFormerSigner (saved);
    }
    return toResponse (saved);
  }

  /**
   * 解綁名下 active 簽名人：user_id 清空、former_user_id 留存，id 不變。
   * 降權呼叫時維持 active（候選仍可見）；停用呼叫時一併停用。
   */
  private void unlinkUserSigners (User user, boolean deactivate) {
    for (var signer : identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (user.getUserId ())) {
      signer.setUser (null);
      signer.setFormerUser (user);
      if (deactivate) signer.setActive (false);
    }
  }

  private Long currentUserId () {
    var auth = SecurityContextHolder.getContext ().getAuthentication ();
    if (auth != null && auth.getPrincipal () instanceof UserPrincipal p) {
      return p.getUserId ();
    }
    return null;
  }

  /** 管理者重設密碼 */
  @Transactional
  public void resetPassword (Long userId, String newPassword) {
    User user = userRepository.findById (userId)
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    user.setPassword (passwordEncoder.encode (newPassword));
    userRepository.save (user);
  }

  private UserResponse toResponse (User user) {
    return new UserResponse (user.getUserId (),
        user.getUsername (),
        user.getDisplayName (),
        user.getEmail (),
        user.getRole ().name (),
        user.isActive ());
  }
}