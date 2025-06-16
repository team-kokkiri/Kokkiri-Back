package com.example.kokkiri.member.domain.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class MemberLoginRequest {
    private String email;
    private String password;
}
