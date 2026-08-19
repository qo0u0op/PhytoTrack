package com.d0w0b.phytotrack.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.UserRepository;

/**
 * 啟動時資料初始化（Data Initializer）
 *
 * 若資料庫沒有管理者/員工帳號，則依 application.yaml 的 app.bootstrap
 * 建立預設帳號（可覆寫），方便首次登入。
 */
@Configuration
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final IdentifierRepository identifierRepository;
  private final PasswordEncoder passwordEncoder;
  private final String adminUsername;
  private final String adminPassword;
  private final String staffUsername;
  private final String staffPassword;
  private final String viewerUsername;
  private final String viewerPassword;

  public DataInitializer(UserRepository userRepository,
                         IdentifierRepository identifierRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.bootstrap.admin-username}") String adminUsername,
                         @Value("${app.bootstrap.admin-password}") String adminPassword,
                         @Value("${app.bootstrap.staff-username}") String staffUsername,
                         @Value("${app.bootstrap.staff-password}") String staffPassword,
                         @Value("${app.bootstrap.viewer-username}") String viewerUsername,
                         @Value("${app.bootstrap.viewer-password}") String viewerPassword) {
    this.userRepository = userRepository;
    this.identifierRepository = identifierRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
    this.staffUsername = staffUsername;
    this.staffPassword = staffPassword;
    this.viewerUsername = viewerUsername;
    this.viewerPassword = viewerPassword;
  }

  @Override
  public void run(String... args) {
    // 僅在使用者不存在時建立，避免覆寫或重複
    User admin = getOrCreateUser(adminUsername, "管理員", adminPassword, User.Role.ROLE_ADMIN);
    User staff = getOrCreateUser(staffUsername, "診斷員", staffPassword, User.Role.ROLE_STAFF);
    // 檢視者帳號：供開發環境驗證 VIEWER 角色的遮蔽行為（如送件人個人資料）
    getOrCreateUser(viewerUsername, "檢視員", viewerPassword, User.Role.ROLE_VIEWER);

    // 診斷簽名人（Identifier）需關聯系統使用者，故無法於 schema.sql 預填，
    // 改在此處建立；僅在完全沒有簽名人時才建立。
    if (identifierRepository.count() == 0) {
      createIdentifier("張志明", staff);
      createIdentifier("林雅惠", staff);
      createIdentifier("陳建宏", admin);
    }
  }

  private User getOrCreateUser(String username, String displayName, String password, User.Role role) {
    return userRepository.findByUsername(username).orElseGet(() -> {
      User user = new User();
      user.setUsername(username);
      user.setDisplayName(displayName);
      user.setPassword(passwordEncoder.encode(password));
      user.setRole(role);
      user.setActive(true);
      return userRepository.save(user);
    });
  }

  private void createIdentifier(String name, User user) {
    Identifier identifier = new Identifier();
    identifier.setIdentifier(name);
    identifier.setUser(user);
    identifierRepository.save(identifier);
  }
}