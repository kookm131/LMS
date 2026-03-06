package com.example.LMS.security.service;

import com.example.LMS.user.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OAuth2UserUpsertService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuth2UserUpsertService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialProfile profile = extractProfile(registrationId, oauth.getAttributes());

        if (profile.email == null || profile.email.isBlank()) {
            throw new OAuth2AuthenticationException("OAuth 이메일을 가져오지 못했습니다.");
        }

        UserRepository.UserAuthInfo user = userRepository.findAuthInfoByEmail(profile.email)
                .orElseGet(() -> {
                    String generatedUsername = generateUsername(registrationId, profile.email);
                    String safeUsername = ensureUniqueUsername(generatedUsername);
                    userRepository.saveSocialUser(safeUsername,
                            passwordEncoder.encode(UUID.randomUUID().toString()),
                            profile.email,
                            profile.name == null || profile.name.isBlank() ? safeUsername : profile.name);
                    return userRepository.findAuthInfoByEmail(profile.email)
                            .orElseThrow(() -> new UsernameNotFoundException("소셜 사용자 저장 실패"));
                });

        if (profile.name != null && !profile.name.isBlank() && !profile.name.equals(user.name())) {
            userRepository.updateNameByEmail(profile.email, profile.name);
            user = userRepository.findAuthInfoByEmail(profile.email).orElse(user);
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role()));
        Map<String, Object> attrs = new HashMap<>(oauth.getAttributes());
        attrs.put("username", user.username());
        attrs.put("email", user.email());
        attrs.put("name", user.name());

        return new DefaultOAuth2User(authorities, attrs, "username");
    }

    private SocialProfile extractProfile(String registrationId, Map<String, Object> attributes) {
        if ("naver".equalsIgnoreCase(registrationId)) {
            Object responseObj = attributes.get("response");
            if (responseObj instanceof Map<?, ?> response) {
                String email = asString(response.get("email"));
                String name = asString(response.get("name"));
                return new SocialProfile(email, name);
            }
        }

        String email = asString(attributes.get("email"));
        String name = asString(attributes.get("name"));
        return new SocialProfile(email, name);
    }

    private String generateUsername(String provider, String email) {
        String local = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        if (local.isBlank()) local = "user";
        String base = (provider + "_" + local).toLowerCase(Locale.ROOT);
        return base.length() > 50 ? base.substring(0, 50) : base;
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int n = 1;
        while (userRepository.countByUsername(candidate) > 0) {
            String suffix = "_" + n++;
            int maxBaseLen = Math.max(1, 50 - suffix.length());
            String trimmed = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            candidate = trimmed + suffix;
        }
        return candidate;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record SocialProfile(String email, String name) {}
}
