package com.example.kokkiri.notification.dto;

import com.example.kokkiri.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationDto {
    private String content;
    private String url;
    private NotificationType notificationType;
    private LocalDateTime actionCreatedAt;
}
