package com.example.kokkiri.member.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class MemberLoginReqDto {
    private String email;
    private String password;
}
