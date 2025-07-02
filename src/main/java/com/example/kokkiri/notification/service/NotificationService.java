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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
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
public class NotificationService {

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ChatInvitationRepository chatInvitationRepository;

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간

    public SseEmitter subscribe(String lastEventId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = getCurrentMember(email);
        String emitterId = member.getId() + "_" + System.currentTimeMillis();
        SseEmitter sseEmitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        setupSseCallbacks(sseEmitter, emitterId);

        // 초기 연결 시 클라이언트에게 연결 성공 메시지 전송
        sendConnectionComment(sseEmitter, member.getId(), emitterId);

        // 유실된 이벤트가 있다면 전송
        resendLostEvents(sseEmitter, lastEventId, member.getId());

        return sseEmitter;
    }

//    @Transactional
//    public void send(Member receiver, NotificationType notificationType, String content, String url, Long invitationId, LocalDateTime actionCreatedAt) {
//        Notification notification = notificationRepository.save(createNotification(receiver, notificationType, content, url, invitationId, actionCreatedAt));
//        String memberId = String.valueOf(receiver.getId());
//
//        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
//        sseEmitters.forEach((emitterId, emitter) -> {
//            emitterRepository.saveEventCache(emitterId, notification);
//            sendNotificationToClient(emitter, emitterId, notification);
//        });
//    }

    // notification 저장
    @Transactional
    public Notification createAndSaveNotification(Member receiver, NotificationType notificationType, String content, String url, Long invitationId, LocalDateTime actionCreatedAt) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .url(url)
                .invitationId(invitationId)
                .actionCreatedAt(actionCreatedAt)
                .build();
        return notificationRepository.save(notification);
    }

    // 클라이언트에게 notification 전송
    public void send(Member receiver, NotificationType notificationType, String content, String url, Long invitationId, LocalDateTime actionCreatedAt) {
        // Step 1. DB에 알림 저장 (트랜잭션 완료)
        Notification notification = createAndSaveNotification(receiver, notificationType, content, url, invitationId, actionCreatedAt);

        // Step 2. Emitter 조회 및 전송 (트랜잭션과 무관)
        String memberId = String.valueOf(receiver.getId());
        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithByMemberId(memberId);
        sseEmitters.forEach((emitterId, emitter) -> {
            emitterRepository.saveEventCache(emitterId, notification);
            sendNotificationToClient(emitter, emitterId, notification);
        });
    }

    // notification 리스트 조회
    @Transactional(readOnly = true)
    public NotificationPageResDto getNotifications(Long lastId, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = getCurrentMember(email);
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


    @Transactional(readOnly = true)
    public Optional<Notification> findByInvitationId(Long invitationId) {
        return notificationRepository.findByInvitationId(invitationId);
    }

    @Transactional
    public void markChatNotificationsAsRead() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = getCurrentMember(email);
        notificationRepository.deleteAllNotificationsByType(member, NotificationType.CHAT);
    }

    @Transactional
    public void markAllAsRead() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = getCurrentMember(email);
        notificationRepository.deleteAllNonChatNotificationsForUser(member);
        chatInvitationRepository.softDeleteAllByInvitedMemberId(member.getId());
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = getCurrentMember(email);

        // 1. 알림 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + notificationId + "에 해당하는 알림을 찾을 수 없습니다."));

        // 2. 알림 소유권 확인
        if (!notification.getReceiver().getId().equals(member.getId())) {
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
        notificationRepository.softDeleteByIdAndMember(notificationId, member);
    }

    public Member getCurrentMember(String email) {
        Member member = memberRepository.findByEmailAndIsDeleted(email,"N")
                .orElseThrow(() -> new EntityNotFoundException("Member not found with email: " + email));
        return member;
    }

    @Scheduled(fixedRate = 30000) // 30초마다 실행
    public void sendHeartbeat() {
        // 현재 저장된 모든 Emitter에 대해 반복
        emitterRepository.findAll().forEach((id, emitter) -> {
            try {
                // "heartbeat" 라는 이름의 더미 이벤트 전송
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .comment("keeping connection alive"));

                log.info("Sent heartbeat to emitterId: {}", id);

            } catch (IOException e) {
                // IOExcepion이 발생하면 연결이 끊긴 것이므로,
                // 저장소에서 해당 Emitter를 즉시 삭제한다.
                log.warn("Heartbeat failed for emitterId: {}. Removing emitter.", id);
                emitterRepository.deleteById(id);
            }
        });
    }


    // =================  PRIVATE HELPER METHODS  ================= //

    // emitter 정리 로직 등록
    private void setupSseCallbacks(SseEmitter sseEmitter, String emitterId) {
        Runnable cleanup = () -> {
            try {
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

    // 최초 연결시 메세지 전송
    // 처음 연결이 수립되고 아무 데이터도 보내지 않으면 503 오류가 발생할 수 있어 데이터를 전송함
    private void sendConnectionComment(SseEmitter sseEmitter, Long memberId, String emitterId) {
        try {
            sseEmitter.send(SseEmitter.event().comment("EventStream Connected. [memberId=" + memberId + "]"));
            log.info("SSE connection comment sent. emitterId: {}", emitterId);
        } catch (IOException e) {
            log.info("Failed to send connection comment for emitterId: {}", emitterId, e);
            emitterRepository.deleteById(emitterId);
        }
    }

    // 클라이언트가 연결이 끊겼다가 다시 접속했을 때, 놓친 데이터가 있는지 확인하고 전송
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
        } catch (AsyncRequestNotUsableException e) {
            log.debug("Client disconnected: AsyncRequestNotUsableException, emitterId: {}", emitterId);
            emitter.complete(); // 자원을 정리하고 연결 종료 처리
            emitterRepository.deleteById(emitterId);
        } catch (IOException e) {
            log.warn("IOException (Broken pipe or other), emitterId: {}", emitterId, e);
            emitter.complete(); // 자원을 정리하고 연결 종료 처리
            emitterRepository.deleteById(emitterId);
        } catch (Exception e) {
            log.error("Unexpected error on emitterId: {}", emitterId, e);
            emitterRepository.deleteById(emitterId);
        }
    }
}