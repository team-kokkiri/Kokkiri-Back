package com.example.kokkiri.notification.repository;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.notification.domain.Notification;
import com.example.kokkiri.notification.domain.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.receiver.id = :memberId
              AND n.delYn = 'N'
              AND n.notificationType != com.example.kokkiri.notification.domain.NotificationType.CHAT
              AND (:lastId IS NULL OR n.id < :lastId)
            ORDER BY
              CASE WHEN n.notificationType = com.example.kokkiri.notification.domain.NotificationType.INVITATION THEN 0 ELSE 1 END,
              n.actionCreatedAt DESC
            """)
    List<Notification> findNextPageByMemberId(
            @Param("memberId") Long memberId,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    Long countByReceiverIdAndDelYn(Long memberId, String delYn);

    Slice<Notification> findByReceiverIdAndDelYnAndIdLessThanOrderByIdDesc(Long receiverId, String delYn, Long lastId, Pageable pageable);

    Optional<Notification> findByInvitationId(Long invitationId);

    // ✨ Enum 타입 사용 및 반환타입 int로 통일
    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.delYn = 'Y' WHERE n.receiver = :member AND n.notificationType = :type")
    int deleteAllNotificationsByType(@Param("member") Member member, @Param("type") NotificationType type);

    // ✨ 반환 타입을 int로 변경하고 Enum 타입 사용
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.delYn = 'Y' WHERE n.receiver = :member AND n.notificationType <> com.example.kokkiri.notification.domain.NotificationType.CHAT")
    int deleteAllNonChatNotificationsForUser(@Param("member") Member member);

    // ✨ 반환 타입을 int로 변경하여 안정성 확보
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.delYn = 'Y' WHERE n.id = :notificationId AND n.receiver = :member")
    int softDeleteByIdAndMember(@Param("notificationId") Long notificationId, @Param("member") Member member);
}