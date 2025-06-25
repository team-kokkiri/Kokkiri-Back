package com.example.kokkiri.notification.dto;

import com.example.kokkiri.notification.domain.Notification;
import com.example.kokkiri.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String content;
    private String url;
    private NotificationType notificationType;
    private Long invitationId;
    private LocalDateTime actionCreatedAt;
    private String isRead;

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .url(notification.getUrl())
                .notificationType(notification.getNotificationType())
                .invitationId(notification.getInvitationId())
                .actionCreatedAt(notification.getActionCreatedAt())
                .isRead(notification.getIsRead())
                .build();
    }
}
