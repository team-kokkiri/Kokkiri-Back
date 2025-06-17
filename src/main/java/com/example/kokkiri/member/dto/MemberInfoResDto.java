package com.example.kokkiri.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberInfoResDto {
    private String email;
    private String nickname;
    private String role;
}