package com.example.kokkiri.common.domain.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String email;
}
