package com.example.kokkiri.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberInfoResDto {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String avatar; // avatarUrl에서 avatar로 변경
}