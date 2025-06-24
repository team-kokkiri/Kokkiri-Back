package com.example.kokkiri.notification.service;

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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
//    private final TaskScheduler taskScheduler; // Spring의 공유 TaskScheduler 주입

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000; // 1시간
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;

    /**
     * 클라이언트가 SSE를 구독하는 메서드.
     * 이 메서드는 트랜잭션을 사용하지 않아야 커넥션 누수를 막을 수 있다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SseEmitter subscribe(String lastEventId) {
        Member member = getCurrentMember();
        String emitterId = member.getId() + "_" + System.currentTimeMillis();
        SseEmitter sseEmitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        // ★★★ 1. 개별 스케줄러 생성 ★★★
        ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

        // SSE 연결 콜백 설정 (스케줄러 종료 로직 포함)
        setupSseCallbacks(sseEmitter, emitterId, heartbeatScheduler);

        // 초기 연결 시 클라이언트에게 연결 성공 메시지 전송
        sendConnectionComment(sseEmitter, member.getId(), emitterId);

        // ★★★ 2. 생성한 스케줄러로 Heartbeat 전송 예약 ★★★
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                sseEmitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                // Broken pipe 등 IO 관련 예외 발생 시,
                // 클라이언트 연결이 끊어진 것으로 간주하고 emitter를 완료시킴.
                // onCompletion 콜백이 호출되어 리소스가 정리됨.
                log.warn("Heartbeat failed for emitterId: {}. Completing emitter.", emitterId);
            }
        }, 0, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 유실된 이벤트가 있다면 전송
        resendLostEvents(sseEmitter, lastEventId, member.getId());

        return sseEmitter;
    }

    /**
     * 알림을 생성하고 해당 사용자에게 전송합니다.
     */
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

    /**
     * 특정 사용자의 알림 목록을 페이지네이션하여 조회합니다. (무한 스크롤)
     */
    // NotificationService.java 내의 getNotifications 메서드

    @Transactional(readOnly = true)
    public NotificationPageResDto getNotifications(Long lastId, int size) {
        Member member = getCurrentMember();
        // 첫 페이지 조회 시 lastId가 null이므로, Long의 최대값을 사용하여 모든 알림을 대상으로 하도록 함
        Long effectiveLastId = (lastId == null) ? Long.MAX_VALUE : lastId;
        Pageable pageable = PageRequest.of(0, size);

        Slice<Notification> notificationSlice = notificationRepository.findByReceiverIdAndDelYnAndIdLessThanOrderByIdDesc(
                member.getId(), "N", effectiveLastId, pageable);

        // 조회된 엔티티 리스트를 DTO 리스트로 변환
        List<NotificationDto> dtos = notificationSlice.getContent().stream()
                .map(NotificationDto::from)
                .collect(Collectors.toList());

        // 1. 다음 페이지 조회를 위한 마지막 ID 계산
        Long newLastId = null;
        if (!dtos.isEmpty()) {
            newLastId = dtos.get(dtos.size() - 1).getId();
        }

        // 2. 읽지 않은 알림 총 개수 조회
        Long totalUnreadCount = notificationRepository.countByReceiverIdAndDelYnAndIsRead(member.getId(), "N", "N");

        // 3. DTO의 모든 필드를 포함하여 객체 생성 후 반환
        return new NotificationPageResDto(dtos, notificationSlice.hasNext(), newLastId, totalUnreadCount);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = findNotificationById(notificationId);
        notification.delete();
    }

    @Transactional
    public void updateNotificationReadStatus(Long notificationId) {
        Notification notification = findNotificationById(notificationId);
        notification.updateIsRead();
    }

    public Optional<Notification> findByInvitationId(Long invitationId) {
        return notificationRepository.findByInvitationId(invitationId);
    }

    @Transactional
    public void markChatNotificationsAsRead() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with email: " + email));

        notificationRepository.markAllChatNotificationsAsReadForMember(member.getId());
    }


    // =================  PRIVATE HELPER METHODS  ================= //

    private Member getCurrentMember() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Member not found with email: " + email));
    }

    private Notification findNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + notificationId));
    }

    private void setupSseCallbacks(SseEmitter sseEmitter, String emitterId, ScheduledExecutorService scheduler) {
        Runnable cleanup = () -> {
            // ★★★ 3. 스케줄러 종료 ★★★
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                log.info("Scheduler for emitterId {} has been shut down.", emitterId);
            }
            emitterRepository.deleteById(emitterId);
            log.info("Cleaned up resources for emitterId: {}", emitterId);
        };

        sseEmitter.onCompletion(cleanup);
        sseEmitter.onTimeout(cleanup);
        sseEmitter.onError(e -> {
            log.error("SSE Error for emitterId: {}", emitterId, e);
            cleanup.run();
        });
    }

    private void sendConnectionComment(SseEmitter sseEmitter, Long memberId, String emitterId) {
        try {
            sseEmitter.send(SseEmitter.event().comment("EventStream Connected. [memberId=" + memberId + "]"));
            log.info("SSE connection comment sent. emitterId: {}", emitterId);
        } catch (IOException e) {
            log.warn("Failed to send connection comment for emitterId: {}", emitterId, e);
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
            log.error("Failed to send SSE event for emitterId: {}", emitterId, e);
            emitterRepository.deleteById(emitterId); // 에러 발생 시 해당 Emitter 제거
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