package com.example.kokkiri.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String role;
    private String avatar;

    public JwtResponse(String accessToken, String refreshToken, String email, String role) {
        this(accessToken, refreshToken, email, role, null); // avatar는 null로 처리
    }
}
