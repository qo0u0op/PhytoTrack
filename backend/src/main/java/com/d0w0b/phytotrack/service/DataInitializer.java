package com.d0w0b.phytotrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.CaseRepository;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.UserRepository;

/**
 * 啟動時資料初始化 (Data Initializer)
 *
 * 依環境決定預設帳號：
 * - dev/test：建立 admin/staff/viewer 三帳號，方便開發與 VIEWER 遮蔽驗證
 * - prod：僅建立 admin 單一帳號與其簽名人，避免預設 staff/viewer 落入正式環境
 */
@Configuration
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final IdentifierRepository identifierRepository;
  private final CaseRepository caseRepository;
  private final PasswordEncoder passwordEncoder;
  private final String adminUsername;
  private final String adminPassword;
  private final String staffUsername;
  private final String staffPassword;
  private final String viewerUsername;
  private final String viewerPassword;
  private final Environment environment;

  public DataInitializer (UserRepository userRepository,
                         IdentifierRepository identifierRepository,
                         CaseRepository caseRepository,
                         PasswordEncoder passwordEncoder,
                         Environment environment,
                         @Value ("${app.bootstrap.admin-username}") String adminUsername,
                         @Value ("${app.bootstrap.admin-password}") String adminPassword,
                         @Value ("${app.bootstrap.staff-username}") String staffUsername,
                         @Value ("${app.bootstrap.staff-password}") String staffPassword,
                         @Value ("${app.bootstrap.viewer-username}") String viewerUsername,
                         @Value ("${app.bootstrap.viewer-password}") String viewerPassword) {
    this.userRepository = userRepository;
    this.identifierRepository = identifierRepository;
    this.caseRepository = caseRepository;
    this.passwordEncoder = passwordEncoder;
    this.environment = environment;
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
    this.staffUsername = staffUsername;
    this.staffPassword = staffPassword;
    this.viewerUsername = viewerUsername;
    this.viewerPassword = viewerPassword;
  }

  @Override
  public void run (String... args) {
    boolean isProd = environment.acceptsProfiles (Profiles.of ("prod"));
    // 僅在使用者不存在時建立，避免覆寫或重複
    // prod：僅 admin 且固定 admin/admin123，忽略 config 的 bootstrap，避免透過 TOML 改 prod 密碼
    if (isProd) {
      getOrCreateUser ("admin", "管理員", "admin123", User.Role.ROLE_ADMIN);
    } else {
      getOrCreateUser (adminUsername, "管理員", adminPassword, User.Role.ROLE_ADMIN);
      getOrCreateUser (staffUsername, "診斷員", staffPassword, User.Role.ROLE_STAFF);
      // 檢視者帳號：供開發環境驗證 VIEWER 角色的遮蔽行為 (如送件人個人資料)
      getOrCreateUser (viewerUsername, "檢視員", viewerPassword, User.Role.ROLE_VIEWER);
    }

    // 1. 移除預設 3 筆種子（BREAKING）：既有庫若存在則置 active=false（若未被引用則刪除）
    for (String name : new String[]{"張志明", "林雅惠", "陳建宏"}) {
      for (Identifier i : identifierRepository.findAll ()) {
        if (name.equals (i.getIdentifier ())) {
          if (caseRepository.existsByCaseIdentifiersIdentifierIdentifierId (i.getIdentifierId ())) {
            if (i.isActive ()) {
              i.setActive (false);
              identifierRepository.save (i);
            }
          } else {
            identifierRepository.delete (i);
          }
        }
      }
    }
    // 2. 補建：確保所有 STAFF/ADMIN 至少有一個以 displayName 命名的簽名人（獨立於通用）
    for (User user : userRepository.findAll ()) {
      if ((user.getRole () == User.Role.ROLE_STAFF || user.getRole () == User.Role.ROLE_ADMIN)) {
        boolean hasActiveWithName = identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (user.getUserId ()).stream ()
            .anyMatch (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), user.getDisplayName ()));
        if (!hasActiveWithName) {
          Identifier identifier = new Identifier ();
          identifier.setIdentifier (user.getDisplayName ());
          identifier.setUser (user);
          identifier.setActive (true);
          identifierRepository.save (identifier);
        }
      }
    }
  }

  private User getOrCreateUser (String username, String displayName, String password, User.Role role) {
    return userRepository.findByUsername (username).orElseGet (() -> {
      User user = new User ();
      user.setUsername (username);
      user.setDisplayName (displayName);
      user.setPassword (passwordEncoder.encode (password));
      user.setRole (role);
      user.setActive (true);
      return userRepository.save (user);
    });
  }

  private void createIdentifier (String name, User user) {
    Identifier identifier = new Identifier ();
    identifier.setIdentifier (name);
    identifier.setUser (user);
    identifier.setActive (true);
    identifierRepository.save (identifier);
  }
}