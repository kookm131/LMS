package com.example.LMS.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인 페이지 Controller
 */
@Controller
public class LoginController {

    // 사용처: 로그인 화면 기능 (GetMapping /login)
    @GetMapping("/login")
    public String login() {
        return "user/login";
    }
}
