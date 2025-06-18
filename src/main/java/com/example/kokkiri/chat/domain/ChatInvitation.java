package com.example.kokkiri.chat.domain;

import com.example.kokkiri.common.domain.BaseTimeEntity;
import com.example.kokkiri.member.domain.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class ChatInvitation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inviter_id")
    private Long inviterId;

    @ManyToOne
    @JoinColumn(name = "invited_member_id", nullable = false)
    private Member invitedMember;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(length = 1)
    @Builder.Default
    private String delYn = "N";


    public void delete() {
        this.delYn = "Y";
    }

}
