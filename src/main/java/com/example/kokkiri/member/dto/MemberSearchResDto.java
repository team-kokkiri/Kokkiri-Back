package com.example.kokkiri.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberSearchResDto {
    private Long memberId;
    private String nickname;
    private String email;
}
