package com.example.kokkiri.notification.controller;

import com.example.kokkiri.chat.dto.MyChatListResDto;
import com.example.kokkiri.notification.domain.Notification;
import com.example.kokkiri.notification.dto.NotificationDto;
import com.example.kokkiri.notification.service.NotificationService;
import org.hibernate.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        return notificationService.subscribe(lastEventId);
    }

    // 내 알림 리스트
    @GetMapping("/list")
    public ResponseEntity<?> getMyNotifications(){
        List<NotificationDto> notificationDtos = notificationService.getNotifications();
        return new ResponseEntity<>(notificationDtos, HttpStatus.OK);
    }

    // 알림 삭제
    @PostMapping("/delete/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId){
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    // 알림 읽음 처리
    @PostMapping("/update/read/{notificationId}")
    public ResponseEntity<?> updateNotificationReadStatus(@PathVariable Long notificationId){
        notificationService.updateNotificationReadStatus(notificationId);
        return ResponseEntity.ok().build();
    }


}
