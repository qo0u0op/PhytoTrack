package com.d0w0b.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.d0w0b.models.User;
import com.d0w0b.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

  @Autowired
  private UserService userService;

  @GetMapping("/register")
  public String registerPage(Model model, User user) {
    model.addAttribute("user", user);
    return "user/register";
  }

  @PostMapping("/register")
  public String register(Model model, User user) {
    try {
      userService.register(user);
      model.addAttribute("msg", "註冊成功，請登入");
      model.addAttribute("success", true);
    } catch (Exception e) {
      model.addAttribute("msg", "註冊失敗：" + e.getMessage());
      model.addAttribute("success", false);
    }
    model.addAttribute("user", user);
    return "user/register";
  }

  @GetMapping("/login")
  public String loginPage(Model model, User user) {
    model.addAttribute("user", user);
    return "user/login";
  }

  @PostMapping("/login")
  public String login(Model model, User user, HttpSession session) {
    User authenticatedUser = userService.login(user.getUsername(), user.getPassword());
    if (authenticatedUser != null) {
      session.setAttribute("user", authenticatedUser);
      model.addAttribute("msg", "歡迎回來，" + authenticatedUser.getDisplayName());
      model.addAttribute("success", true);
    } else {
      model.addAttribute("msg", "帳號或密碼錯誤");
      model.addAttribute("success", false);
    }
    model.addAttribute("user", user);
    return "user/login";
  }

  @GetMapping("/logout")
  public String logoutPage(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }
}
