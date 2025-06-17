package com.example.kokkiri.notification.service;

import com.example.kokkiri.chat.domain.ChatInvitation;
import com.example.kokkiri.chat.domain.ChatRoom;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.notification.domain.Notification;
import com.example.kokkiri.notification.domain.NotificationType;
import com.example.kokkiri.notification.dto.NotificationDto;
import com.example.kokkiri.notification.repository.EmitterRepository;
import com.example.kokkiri.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.websocket.OnClose;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
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

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    public SseEmitter subscribe(String lastEventId){
        try {
            Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member cannot be found"));
            Long memberId = member.getId();

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

            sseEmitter.onCompletion(() -> {
                heartbeatScheduler.shutdown();
                emitterRepository.deleteById(emitterId);
            });

            sseEmitter.onTimeout(() -> {
                heartbeatScheduler.shutdown();
                emitterRepository.deleteById(emitterId);
            });

            sendToClient(sseEmitter, emitterId, "EventStream Created. [memberId=" + memberId + "]");

            if (!lastEventId.isEmpty()) {
                Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(memberId));
                events.entrySet().stream()
                        .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                        .forEach(entry -> sendToClient(sseEmitter, entry.getKey(), entry.getValue()));
            }

            return sseEmitter;
        } catch (Exception e) {
            System.out.println("SSE 연결 중 예외 발생!: " + e.getMessage());
            throw e;
        }
    }

    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            Object payload = data;

            if (data instanceof Notification notification) {
                payload = NotificationDto.builder()
                        .content(notification.getContent().getContent())
                        .url(notification.getUrl().getUrl())
                        .toName(notification.getToName())
                        .notificationType(notification.getNotificationType())
                        .actionCreatedAt(notification.getActionCreatedAt())
                        .build();
            }

            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(payload));

            System.out.println("✅ [SSE 전송 완료] emitterId: " + emitterId + ", data: " + payload);

        } catch (IOException exception) {
            System.out.println("❌ [SSE 전송 실패] emitterId: " + emitterId + ", 이유: " + exception.getMessage());
            emitterRepository.deleteById(emitterId);
            throw new RuntimeException("SSE 연결 오류!" ,exception);
        }
    }

    public void send(Member receiver, NotificationType notificationType, String content, String url, LocalDateTime actionCreatedAt){
        Notification notification = notificationRepository.save(createNotification(receiver, notificationType, content, url, actionCreatedAt));
        String memberId = String.valueOf(receiver.getId());

        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
        sseEmitters.forEach(
                (key, emitter) -> {
                    emitterRepository.saveEventCache(key, notification);
                    sendToClient(emitter, key, notification);
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

//    // 그룹 채팅 초대 알림
//    public void sendInvitationNotification(ChatInvitation chatInvitation, String nickname){
//        Member invitedMember = chatInvitation.getInvitedMember();
//        String chatRoomName = chatInvitation.getChatRoom().getName();
//        String content = nickname + "님이 <" + chatRoomName + "> 그룹 채팅에 초대하였습니다.";
//
//        send(invitedMember, NotificationType.INVITATION, content, null, nickname);
//    }



}
