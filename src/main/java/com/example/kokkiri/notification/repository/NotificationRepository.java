package com.example.kokkiri.notification.repository;

import com.example.kokkiri.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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


}
