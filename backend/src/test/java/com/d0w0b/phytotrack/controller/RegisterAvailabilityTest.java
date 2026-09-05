package com.d0w0b.phytotrack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.d0w0b.phytotrack.config.RateLimitFilter;
import com.d0w0b.phytotrack.config.SecurityConfig;
import com.d0w0b.phytotrack.dto.AuthDtos.RegisterRequest;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.DeactivateRequestRepository;
import com.d0w0b.phytotrack.repository.UserRepository;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.service.AuthService;

/**
 * 註冊可用性測試：註冊信箱查重與公開可用性查詢端點
 */
@WebMvcTest (AuthController.class)
@Import (SecurityConfig.class)
class RegisterAvailabilityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private com.d0w0b.phytotrack.service.AccountService accountService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean
  private RateLimitFilter rateLimitFilter;

  @BeforeEach
  void setUpFilterToPassThrough () throws Exception {
    doAnswer (invocation -> {
      FilterChain chain = invocation.getArgument (2, FilterChain.class);
      chain.doFilter (invocation.getArgument (0, HttpServletRequest.class),
          invocation.getArgument (1, HttpServletResponse.class));
      return null;
    }).when (jwtAuthenticationFilter).doFilter (any (), any (), any ());
    doAnswer (invocation -> {
      FilterChain chain = invocation.getArgument (2, FilterChain.class);
      chain.doFilter (invocation.getArgument (0, HttpServletRequest.class),
          invocation.getArgument (1, HttpServletResponse.class));
      return null;
    }).when (rateLimitFilter).doFilter (any (), any (), any ());
  }

  @Test
  void checkUsername_taken_shouldReturnFalseWithoutLogin () throws Exception {
    when (authService.isUsernameAvailable ("admin")).thenReturn (false);

    // 未登入即可呼叫（公開端點），僅回布林值
    mockMvc.perform (get ("/api/auth/check-username").param ("username", "admin"))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.available").value (false));
  }

  @Test
  void checkUsername_free_shouldReturnTrue () throws Exception {
    when (authService.isUsernameAvailable ("fresh-user")).thenReturn (true);

    mockMvc.perform (get ("/api/auth/check-username").param ("username", "fresh-user"))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.available").value (true));
  }

  @Test
  void checkEmail_taken_shouldReturnFalseWithoutLogin () throws Exception {
    when (authService.isEmailAvailable ("taken@mail.com")).thenReturn (false);

    mockMvc.perform (get ("/api/auth/check-email").param ("email", "taken@mail.com"))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.available").value (false));
  }

  @Test
  void checkEmail_empty_shouldReturnTrue () throws Exception {
    when (authService.isEmailAvailable ("")).thenReturn (true);

    mockMvc.perform (get ("/api/auth/check-email").param ("email", ""))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.available").value (true));
  }

  /** 註冊信箱查重：Service 層單元測試（Mockito，不需 Spring） */
  @Nested
  @ExtendWith (MockitoExtension.class)
  class RegisterEmailUniqueness {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeactivateRequestRepository deactivateRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp () {
      service = new AuthService (userRepository, deactivateRequestRepository, passwordEncoder, null, null);
    }

    @Test
    void register_duplicateEmail_shouldThrowEmailTaken () {
      when (userRepository.findByUsername ("newbie")).thenReturn (Optional.empty ());
      when (userRepository.existsByEmailIgnoreCase ("TAKEN@mail.com")).thenReturn (true);

      assertThatThrownBy (() -> service.register (new RegisterRequest ("newbie", "新人", "secret123", "TAKEN@mail.com")))
          .isInstanceOf (ApiException.class)
          .satisfies (e -> {
            ApiException ex = (ApiException) e;
            assertThat (ex.getCode ()).isEqualTo ("EMAIL_TAKEN");
            assertThat (ex.getStatus ()).isEqualTo (HttpStatus.CONFLICT);
          });
      verify (userRepository, never ()).save (any (User.class));
    }

    @Test
    void register_emptyEmail_shouldSkipCheckAndSucceed () {
      when (userRepository.findByUsername ("newbie")).thenReturn (Optional.empty ());
      when (userRepository.save (any (User.class))).thenAnswer (invocation -> invocation.getArgument (0));

      service.register (new RegisterRequest ("newbie", "新人", "secret123", "   "));

      verify (userRepository, never ()).existsByEmailIgnoreCase (any ());
      verify (userRepository).save (any (User.class));
    }
  }
}
