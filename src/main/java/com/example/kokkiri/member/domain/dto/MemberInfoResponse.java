package com.example.kokkiri.member.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberInfoResponse {
    private String email;
    private String nickname;
    private String role;
}