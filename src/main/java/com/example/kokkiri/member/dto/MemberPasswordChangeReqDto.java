package com.example.kokkiri.member.dto;

import lombok.Getter;

@Getter
public class MemberPasswordChangeReqDto {
    private String currentPassword;
    private String newPassword;
}
