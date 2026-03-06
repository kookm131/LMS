package com.example.LMS.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 회원가입 폼
 */
public class UserRegisterForm {

    @NotBlank(message = "회원유형은 필수항목입니다")
    private String userType; // NORMAL, BUSINESS

    @NotBlank(message = "아이디는 필수항목입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수항목입니다")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수항목입니다")
    private String passwordConfirm;

    @NotBlank(message = "이메일은 필수항목입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    @NotBlank(message = "이름은 필수항목입니다")
    private String name;

    private String birthDate;

    private String gender; // MALE, FEMALE

    private String businessName;

    @NotBlank(message = "전화번호는 필수항목입니다")
    @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다")
    private String phone;

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
