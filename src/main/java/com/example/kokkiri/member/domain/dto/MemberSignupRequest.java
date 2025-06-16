package com.example.kokkiri.member.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberSignupRequest {
    private String email;
    private String password;
    private String nickname;
    private String teamCode;
}
