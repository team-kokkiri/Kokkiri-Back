package com.example.kokkiri.common.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final Map<String, Object> attributes;
    private final String name; // Spring Security가 내부적으로 쓰는 고유 ID
    private final String email;

    public CustomOAuth2User(Map<String, Object> attributes, String name, String email) {
        this.attributes = attributes;
        this.name = name;
        this.email = email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return name; // 절대 null이 아니어야 함 (고유 식별자)
    }

    public String getEmail() {
        return email;
    }
}
