package com.example.LMS.user.service;

import com.example.LMS.user.dto.*;
import com.example.LMS.user.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 비즈니스 로직
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BusinessUserRepository businessUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       BusinessUserRepository businessUserRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.businessUserRepository = businessUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 회원가입 처리
     */
    @Transactional
    public void register(UserRegisterForm form) {
        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다");
        }

        // DB에는 평문 대신 해시 저장
        String encodedPassword = passwordEncoder.encode(form.getPassword());

        if ("BUSINESS".equals(form.getUserType())) {
            if (form.getBusinessName() == null || form.getBusinessName().isBlank()) {
                throw new IllegalArgumentException("상호명은 필수항목입니다");
            }

            if (businessUserRepository.countByUsername(form.getUsername()) > 0) {
                throw new IllegalArgumentException("이미 사용중인 아이디입니다");
            }

            if (businessUserRepository.countByEmail(form.getEmail()) > 0) {
                throw new IllegalArgumentException("이미 사용중인 이메일입니다");
            }

            businessUserRepository.save(form, encodedPassword);
            return;
        }

        if (form.getBirthDate() == null || form.getBirthDate().isBlank()) {
            throw new IllegalArgumentException("생년월일은 필수항목입니다");
        }

        if (form.getGender() == null || form.getGender().isBlank()) {
            throw new IllegalArgumentException("성별은 필수항목입니다");
        }

        if (userRepository.countByUsername(form.getUsername()) > 0) {
            throw new IllegalArgumentException("이미 사용중인 아이디입니다");
        }

        if (userRepository.countByEmail(form.getEmail()) > 0) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다");
        }

        String role = "STUDENT";
        userRepository.save(form, encodedPassword, role);
    }
}
