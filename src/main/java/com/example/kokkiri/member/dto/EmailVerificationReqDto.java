package com.example.kokkiri.member.dto;

import lombok.Getter;

@Getter
public class EmailVerificationReqDto {
    private String email;
    private String code;
    private String type;
}