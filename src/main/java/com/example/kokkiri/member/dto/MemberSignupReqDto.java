package com.example.kokkiri.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class MemberSignupReqDto {
    private String email;
    private String password;
    private String nickname;
    private String teamCode;
}
