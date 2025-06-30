package com.example.kokkiri.chat.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ChatResDto {
    private Long roomId;
    private String roomName;
}
