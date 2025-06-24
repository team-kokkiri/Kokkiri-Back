package com.example.kokkiri.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberLoginResDto {
    private String accessToken;
    private String email;
    private String role;
    private String avatarUrl;
}
