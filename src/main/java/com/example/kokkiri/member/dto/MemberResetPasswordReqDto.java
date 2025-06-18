package com.example.kokkiri.member.dto;

import lombok.Data;

@Data
public class MemberResetPasswordReqDto {
    private String email;
    private String newPassword;
}
