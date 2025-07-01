package com.example.kokkiri.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long roomId;
    private String message;
    private String senderEmail;
    private String nickname;
    private String avatar;  // 메시지 작성자 아바타 추가
    private LocalDateTime createdTime;

}
