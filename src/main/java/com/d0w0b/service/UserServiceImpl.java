package com.d0w0b.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.d0w0b.models.User;
import com.d0w0b.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public User register(User user) {
    // 檢查使用者名稱是否已存在
    if (userRepository.findByUsername(user.getUsername()).isPresent()) {
      throw new RuntimeException("使用者名稱已存在");
    }
    // 直接儲存密碼
    return userRepository.save(user);
  }

  @Override
  public User login(String username, String password) {
    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      // 直接比對字串
      if (user.getPassword().equals(password)) {
        return user;
      }
    }
    return null;
  }
}
