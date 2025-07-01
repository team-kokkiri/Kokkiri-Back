package com.example.kokkiri.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatMemberDto {
    private Long memberId;
    private String nickname;
    private String avatarUrl;
    private String email;
}
