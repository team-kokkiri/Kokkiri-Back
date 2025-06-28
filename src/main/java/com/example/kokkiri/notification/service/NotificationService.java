package com.example.kokkiri.notification.service;

import com.example.kokkiri.chat.domain.ChatInvitation;
import com.example.kokkiri.chat.repository.ChatInvitationRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.notification.domain.Notification;
import com.example.kokkiri.notification.domain.NotificationType;
import com.example.kokkiri.notification.dto.NotificationDto;
import com.example.kokkiri.notification.dto.NotificationPageResDto;
import com.example.kokkiri.notification.repository.EmitterRepository;
import com.example.kokkiri.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ChatInvitationRepository chatInvitationRepository;


    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SseEmitter subscribe(String lastEventId) {
        Member member = getCurrentMember();
        String emitterId = member.getId() + "_" + System.currentTimeMillis();
        SseEmitter sseEmitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        setupSseCallbacks(sseEmitter, emitterId);

        // 초기 연결 시 클라이언트에게 연결 성공 메시지 전송
        sendConnectionComment(sseEmitter, member.getId(), emitterId);

        // 유실된 이벤트가 있다면 전송
        resendLostEvents(sseEmitter, lastEventId, member.getId());

        return sseEmitter;
    }

    @Transactional
    public void send(Member receiver, NotificationType notificationType, String content, String url, Long invitationId, LocalDateTime actionCreatedAt) {
        Notification notification = notificationRepository.save(createNotification(receiver, notificationType, content, url, invitationId, actionCreatedAt));
        String memberId = String.valueOf(receiver.getId());

        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
        sseEmitters.forEach((emitterId, emitter) -> {
            emitterRepository.saveEventCache(emitterId, notification);
            sendNotificationToClient(emitter, emitterId, notification);
        });
    }

    @Transactional(readOnly = true)
    public NotificationPageResDto getNotifications(Long lastId, int size) {
        Member member = getCurrentMember();
        Long effectiveLastId = (lastId == null) ? Long.MAX_VALUE : lastId;
        Pageable pageable = PageRequest.of(0, size);

        Slice<Notification> notificationSlice = notificationRepository.findByReceiverIdAndDelYnAndIdLessThanOrderByIdDesc(
                member.getId(), "N", effectiveLastId, pageable);

        List<NotificationDto> dtos = notificationSlice.getContent().stream()
                .map(NotificationDto::from)
                .collect(Collectors.toList());

        Long newLastId = null;
        if (!dtos.isEmpty()) {
            newLastId = dtos.get(dtos.size() - 1).getId();
        }

        // CHAT을 제외한 나머지 알림의 개수
        Long totalUnreadCount = notificationRepository.countUnreadNonChatNotifications(member, "N");

        return new NotificationPageResDto(dtos, notificationSlice.hasNext(), newLastId, totalUnreadCount);
    }


    public Optional<Notification> findByInvitationId(Long invitationId) {
        return notificationRepository.findByInvitationId(invitationId);
    }

    @Transactional
    public void markChatNotificationsAsRead() {
        Member member = getCurrentMember();
        notificationRepository.deleteAllNotificationsByType(member, NotificationType.CHAT);
    }

    @Transactional
    public void markAllAsRead() {
        Member member = getCurrentMember();
        notificationRepository.deleteAllNonChatNotificationsForUser(member);
        chatInvitationRepository.softDeleteAllByInvitedMemberId(member.getId());

    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Member currentUser = getCurrentMember();

        // 1. 알림 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + notificationId + "에 해당하는 알림을 찾을 수 없습니다."));

        // 2. 알림 소유권 확인
        if (!notification.getReceiver().getId().equals(currentUser.getId())) {
            throw new SecurityException("알림을 삭제할 권한이 없습니다.");
        }

        // 3. 초대 타입 알림인 경우, 연결된 초대 데이터도 삭제 처리
        if (notification.getNotificationType() == NotificationType.INVITATION) {
            Long invitationId = notification.getInvitationId();
            if (invitationId != null) {
                chatInvitationRepository.findById(invitationId).ifPresent(ChatInvitation::delete);
            }
        }

        // 4. 알림 자체를 삭제 처리
        notificationRepository.softDeleteByIdAndMember(notificationId, currentUser);
    }


    // =================  PRIVATE HELPER METHODS  ================= //

    private Member getCurrentMember() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with email: " + email));
    }

    private void setupSseCallbacks(SseEmitter sseEmitter, String emitterId) {
        Runnable cleanup = () -> {
            try { // ✨ 동일하게 안전장치 추가
                emitterRepository.deleteById(emitterId);
            } catch (Exception e) {
                log.error("Emitter 리소스 정리 중 에러 발생 (emitterId: {})", emitterId, e);
            }
            log.info("Cleaned up resources for emitterId: {}", emitterId);
        };

        sseEmitter.onCompletion(cleanup);
        sseEmitter.onTimeout(cleanup);
        sseEmitter.onError(e -> {
            log.info("SSE Error for emitterId: {}", emitterId, e);
            cleanup.run();
        });
    }

    private void sendConnectionComment(SseEmitter sseEmitter, Long memberId, String emitterId) {
        try {
            sseEmitter.send(SseEmitter.event().comment("EventStream Connected. [memberId=" + memberId + "]"));
            log.info("SSE connection comment sent. emitterId: {}", emitterId);
        } catch (IOException e) {
            log.info("Failed to send connection comment for emitterId: {}", emitterId, e);
            emitterRepository.deleteById(emitterId);
        }
    }

    private void resendLostEvents(SseEmitter sseEmitter, String lastEventId, Long memberId) {
        if (lastEventId != null && !lastEventId.isEmpty()) {
            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByMemberId(String.valueOf(memberId));
            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(entry -> sendNotificationToClient(sseEmitter, entry.getKey(), entry.getValue()));
        }
    }

    private void sendNotificationToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            Object payload = (data instanceof Notification) ? NotificationDto.from((Notification) data) : data;

            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(payload, MediaType.APPLICATION_JSON));

            log.info("SSE event sent. emitterId: {}, data: {}", emitterId, payload);
        } catch (IOException e) {
            log.info("Failed to send SSE event for emitterId: {}", emitterId, e);
            // 리소스를 정리하는 과정에서 발생할 수 있는 모든 예외를 잡아서 처리합니다.
            try {
                emitterRepository.deleteById(emitterId);
            } catch (Exception cleanupException) {
                log.error("SSE emitter 정리 중 예외 발생. emitterId: {}", emitterId, cleanupException);
            }
        }
    }

    private Notification createNotification(Member receiver, NotificationType notificationType, String content, String url, Long invitationId, LocalDateTime actionCreatedAt) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .invitationId(invitationId)
                .actionCreatedAt(actionCreatedAt)
                .build();
    }
}