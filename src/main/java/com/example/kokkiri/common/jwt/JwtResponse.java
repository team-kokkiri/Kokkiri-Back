package com.example.kokkiri.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
}
