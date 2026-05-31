package com.d0w0b.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.d0w0b.models.User;
import com.d0w0b.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

  @Autowired
  private UserService userService;

  @RequestMapping(path = "/register", method = { RequestMethod.GET, RequestMethod.POST })
  public String register(HttpServletRequest request, Model model, User user) {
    if (request.getMethod().equalsIgnoreCase("POST")) {
      try {
        userService.register(user);
        model.addAttribute("msg", "註冊成功，請登入");
        model.addAttribute("success", true);
      } catch (Exception e) {
        model.addAttribute("msg", "註冊失敗：" + e.getMessage());
        model.addAttribute("success", false);
      }
    }
    model.addAttribute("user", user);
    return "user/register";
  }

  @RequestMapping(path = "/login", method = { RequestMethod.GET, RequestMethod.POST })
  public String login(HttpServletRequest request, Model model, User user, HttpSession session) {
    if (request.getMethod().equalsIgnoreCase("POST")) {
      User authenticatedUser = userService.login(user.getUsername(), user.getPassword());
      if (authenticatedUser != null) {
        session.setAttribute("user", authenticatedUser);
        model.addAttribute("msg", "歡迎回來，" + authenticatedUser.getDisplayName());
        model.addAttribute("success", true);
      } else {
        model.addAttribute("msg", "帳號或密碼錯誤");
        model.addAttribute("success", false);
      }
    }
    model.addAttribute("user", user);
    return "user/login";
  }

  @RequestMapping(path = "/logout", method = RequestMethod.GET)
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }
}
