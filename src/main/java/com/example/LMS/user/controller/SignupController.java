package com.example.LMS.user.controller;

import com.example.LMS.user.dto.*;
import com.example.LMS.user.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 회원가입 화면/처리 Controller
 */
@Controller
@RequestMapping("/signup")
public class SignupController {

    private final UserService userService;

    public SignupController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 회원유형 선택 화면 (아이콘 링크)
     */
    // 사용처: 목록/상세 조회 기능 (GetMapping 기본 경로)
    @GetMapping
    public String signupSelect() {
        return "user/signup-select";
    }

    /**
     * 회원가입 폼 화면
     */
    // 사용처: 목록/상세 조회 기능 (GetMapping /form)
    @GetMapping("/form")
    public String signupForm(@RequestParam(defaultValue = "NORMAL") String type, Model model) {
        if (!model.containsAttribute("form")) {
            UserRegisterForm form = new UserRegisterForm();
            form.setUserType(type);
            model.addAttribute("form", form);
        }
        return "user/signup-form";
    }

    /**
     * 회원가입 처리
     */
    // 사용처: 등록/처리 기능 (PostMapping 기본 경로)
    @PostMapping
    public String signupSubmit(
            @Valid @ModelAttribute("form") UserRegisterForm form,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "user/signup-form";
        }

        try {
            userService.register(form);
        } catch (IllegalArgumentException e) {
            // 서비스 계층 검증 메시지를 폼 에러로 노출
            String message = e.getMessage();
            if (message.contains("비밀번호")) {
                bindingResult.rejectValue("passwordConfirm", "invalid", message);
            } else if (message.contains("아이디")) {
                bindingResult.rejectValue("username", "duplicate", message);
            } else if (message.contains("이메일")) {
                bindingResult.rejectValue("email", "duplicate", message);
            } else if (message.contains("상호명")) {
                bindingResult.rejectValue("businessName", "required", message);
            } else {
                bindingResult.reject("signupFailed", message);
            }
            return "user/signup-form";
        }

        return "redirect:/?signup=success";
    }
}
