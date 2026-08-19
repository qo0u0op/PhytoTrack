package com.d0w0b.phytotrack.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.UserRepository;

/**
 * 使用者明細服務（User Details Service）
 *
 * Spring Security 在執行「使用者名稱 + 密碼」認證時，
 * 透過本類別依使用者名稱載入使用者資訊（含角色）。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("使用者不存在：" + username));
    return UserPrincipal.from(user);
  }
}
