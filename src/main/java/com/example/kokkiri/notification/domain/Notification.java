package com.example.kokkiri.notification.domain;

import com.example.kokkiri.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = "receiver")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private String url;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member receiver;

    private LocalDateTime actionCreatedAt;

    @Builder
    public Notification(Member receiver, NotificationType notificationType, String content, String url, LocalDateTime actionCreatedAt){
        this.receiver = receiver;
        this.notificationType = notificationType;
        this.content = content;
        this.url = url;
        this.actionCreatedAt = actionCreatedAt;
    }

}
