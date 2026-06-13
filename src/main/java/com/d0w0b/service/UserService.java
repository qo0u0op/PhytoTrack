package com.d0w0b.service;

import com.d0w0b.models.User;

public interface UserService {
  User register(User user);

  User login(String username, String password);
}
