package com.example.kokkiri.chat.domain;

import com.example.kokkiri.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ChatRoom  extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private String isGroupChat="N";

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatParticipant> chatParticipants = new ArrayList<>();

    // orphanRemoval = true ) 참조 관계가 엮인 요소들을 줄줄이 삭제
    // ChatRoom 삭제시 ChatMessage들이 삭제 되어야 하고, ChatMessage 삭제시 ReadStatus가 삭제되어야 하기 때문에 설정
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatInvitation> chatInvitations = new ArrayList<>();


}
