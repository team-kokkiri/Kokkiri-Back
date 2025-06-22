package com.example.kokkiri.notification.service;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.notification.domain.Notification;
import com.example.kokkiri.notification.domain.NotificationType;
import com.example.kokkiri.notification.dto.NotificationDto;
import com.example.kokkiri.notification.repository.EmitterRepository;
import com.example.kokkiri.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    // 연결 지속시간 = 1시간
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    public SseEmitter subscribe(String lastEventId){
        try {
            // 고유 아이디 생성
            Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member cannot be found"));
            Long memberId = member.getId();

            // emitter 생성 및 저장
            String emitterId = memberId + "_" + System.currentTimeMillis();
            SseEmitter sseEmitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

            // Heartbeat 스케줄러 생성
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

            // Heartbeat 주기적으로 전송 (30초마다)
            heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    sseEmitter.send(SseEmitter.event()
                            .comment("heartbeat"));  // 빈 이벤트 전송
                } catch (IOException e) {
                    heartbeatScheduler.shutdown();
                    emitterRepository.deleteById(emitterId);
                }
            }, 0, 30, TimeUnit.SECONDS);

            // 연결 종료시 heartbeat 스케줄러 종료 & emitter 제거
            // 연결 종료 ) 프론트 EventSource.close(), 서버 emitter.complete 를 호출할 때
            sseEmitter.onCompletion(() -> {
                heartbeatScheduler.shutdown();
                emitterRepository.deleteById(emitterId);
            });

            // 타임아웃시 heartbeat 스케줄러 종료 & emitter 제거
            // 타임 아웃 ) 지정된 시간동안 아무 이벤트가 없을 때
            sseEmitter.onTimeout(() -> {
                heartbeatScheduler.shutdown();
                emitterRepository.deleteById(emitterId);
            });

            // 최초 연결 메세지를 '데이터'가 아닌 '주석'으로 전송
            // 이렇게 하면 클라이언트의 sse 이벤트 리스너가 트리거되지 않아 JSON 파싱 오류가 발생하지 않음
            try {
                sseEmitter.send(SseEmitter.event()
                        .comment("EventStream Connected. [memberId=" + memberId + "]"));
                System.out.println("✅ [SSE 연결 주석 전송 완료] emitterId: " + emitterId);
            } catch (IOException e) {
                System.out.println("❌ [SSE 연결 주석 전송 실패] emitterId: " + emitterId + ", 이유: " + e.getMessage());
                emitterRepository.deleteById(emitterId);
            }


            // lastEventId가 있으면, 유실된 이벤트를 찾아 다시 전송
            if (!lastEventId.isEmpty()) {
                Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(memberId));
                events.entrySet().stream()
                        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                        .forEach(entry -> sendToClient(sseEmitter, entry.getKey(), entry.getValue()));
            }

            // 최종적으로 클라이언트에 emitter를 반환하여 실시간 수신 대기 상태로 진입
            return sseEmitter;
        } catch (Exception e) {
            System.out.println("SSE 연결 중 예외 발생!: " + e.getMessage());
            throw e;
        }
    }

    // SSE emitter를 통해 클라이언트에 실제 데이터를 전송
    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            Object payload = data;

            if (data instanceof Notification notification) {
                payload = NotificationDto.builder()
                        .content(notification.getContent())
                        .url(notification.getUrl())
                        .notificationType(notification.getNotificationType())
                        .actionCreatedAt(notification.getActionCreatedAt())
                        .isRead(notification.getIsRead())
                        .build();
            }


            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(payload, MediaType.APPLICATION_JSON));

            System.out.println("✅[SSE 전송 완료] emitterId: " + emitterId + ", data: " + payload);

        } catch (IOException exception) {
            // 전송 실패시 emitter 제거
            System.out.println("❌[SSE 전송 실패] emitterId: " + emitterId + ", 이유: " + exception.getMessage());
            emitter.completeWithError(exception);
            emitterRepository.deleteById(emitterId);
        }
    }

    // 알림을 특정 사용자에게 전송 & 해당 사용자에게 연결된 모든 SSE emitter에 브로드캐스팅
    public void send(Member receiver, NotificationType notificationType, String content, String url, LocalDateTime actionCreatedAt){
        Notification notification = notificationRepository.save(createNotification(receiver, notificationType, content, url, actionCreatedAt));
        String memberId = String.valueOf(receiver.getId());

        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
        sseEmitters.forEach(
                (key, emitter) -> {
                    try {
                        emitterRepository.saveEventCache(key, notification);
                        sendToClient(emitter, key, notification);
                    } catch (Exception e) {
                        // 하나 실패해도 다른 emitter는 계속 전송되도록
                        System.out.println("❌ emitter 전송 중 일부 실패: " + key + ", 이유: " + e.getMessage());
                    }
                }
        );
    }

    private Notification createNotification(Member receiver, NotificationType notificationType, String content, String url, LocalDateTime actionCreatedAt){
        return Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .actionCreatedAt(actionCreatedAt)
                .build();
    }

    public List<NotificationDto> getNotifications(Long lastId, int size){
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        Pageable pageable = PageRequest.of(0, size);
        List<Notification> notifications = notificationRepository.findNextPageByMemberId(member.getId(), lastId, pageable);

        List<NotificationDto> dtos = new ArrayList<>();
        for (Notification n : notifications){
            NotificationDto dto = NotificationDto.builder()
                    .content(n.getContent())
                    .url(n.getUrl())
                    .notificationType(n.getNotificationType())
                    .actionCreatedAt(n.getActionCreatedAt())
                    .isRead(n.getIsRead())
                    .build();
            dtos.add(dto);
        }

        return dtos;
    }

    public void deleteNotification(Long notificationId){
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(()->new EntityNotFoundException("해당 ID의 알림을 찾을 수 없습니다."));
        notification.delete();
    }

    public void updateNotificationReadStatus(Long notificationId){
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(()->new EntityNotFoundException("해당 ID의 알림을 찾을 수 없습니다."));
        notification.updateIsRead();
    }




}
