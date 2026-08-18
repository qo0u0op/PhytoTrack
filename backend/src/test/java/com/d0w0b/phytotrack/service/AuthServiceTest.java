package com.d0w0b.phytotrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.d0w0b.phytotrack.dto.AuthDtos.AuthResponse;
import com.d0w0b.phytotrack.dto.AuthDtos.LoginRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.RegisterRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.UserRepository;
import com.d0w0b.phytotrack.security.JwtTokenProvider;
import com.d0w0b.phytotrack.security.UserPrincipal;

import java.util.Optional;

/**
 * 認證服務（AuthService）單元測試
 *
 * 使用 Mockito 模擬（Mock）相依物件，只測業務邏輯本身。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  private AuthService authService;

  private User existingUser;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtTokenProvider);
    existingUser = new User();
    existingUser.setUserId(1L);
    existingUser.setUsername("admin");
    existingUser.setDisplayName("管理員");
    existingUser.setPassword("encoded-password");
    existingUser.setRole(User.Role.ROLE_ADMIN);
    existingUser.setActive(true);
  }

  @Test
  void register_shouldEncodePasswordAndAssignViewerRole() {
    when(userRepository.findByUsername("junit-user")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("secret123")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      saved.setUserId(10L);
      return saved;
    });

    UserResponse response = authService.register(
        new RegisterRequest("junit-user", "測試使用者", "secret123", null));

    assertThat(response.username()).isEqualTo("junit-user");
    assertThat(response.role()).isEqualTo("ROLE_VIEWER");
    // 關鍵：儲存前密碼必須已經過 BCrypt 編碼，絕不能存明文
    verify(userRepository).save(any(User.class));
    assertThat(existingUser.getPassword()).isNotEqualTo("secret123");
  }

  @Test
  void register_shouldRejectDuplicateUsername() {
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> authService.register(
        new RegisterRequest("admin", "分身", "secret123", null)))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("USERNAME_TAKEN");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        });
    // 重複帳號不應呼叫 save
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void login_shouldReturnTokenOnSuccess() {
    UserPrincipal principal = UserPrincipal.from(existingUser);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));
    when(jwtTokenProvider.generateToken(existingUser)).thenReturn("jwt-token");

    AuthResponse response = authService.login(new LoginRequest("admin", "secret123"));

    assertThat(response.token()).isEqualTo("jwt-token");
    assertThat(response.user().role()).isEqualTo("ROLE_ADMIN");
  }

  @Test
  void login_shouldThrowWhenUserMissing() {
    UserPrincipal principal = UserPrincipal.from(existingUser);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    // 使用者在載入後被刪除的極端情況：查無此人
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "secret123")))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
  }
}