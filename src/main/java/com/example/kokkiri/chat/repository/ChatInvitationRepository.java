package com.example.kokkiri.chat.repository;

import com.example.kokkiri.chat.domain.ChatInvitation;
import com.example.kokkiri.chat.domain.ChatRoom;
import com.example.kokkiri.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Long> {
    boolean existsByChatRoomAndInvitedMemberAndRespondYnAndDelYn(
            ChatRoom chatRoom, Member invitedMember, String respondYn, String delYn);
}
