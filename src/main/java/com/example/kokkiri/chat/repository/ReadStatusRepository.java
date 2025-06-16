package com.example.kokkiri.chat.repository;

import com.example.kokkiri.chat.domain.ChatRoom;
import com.example.kokkiri.chat.domain.ReadStatus;
import com.example.kokkiri.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {
    List<ReadStatus> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    Long countByChatRoomAndMemberAndIsReadFalse(ChatRoom chatRoom, Member member);
}
