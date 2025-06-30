package com.example.kokkiri.chat.repository;

import com.example.kokkiri.chat.domain.ChatInvitation;
import com.example.kokkiri.chat.domain.ChatRoom;
import com.example.kokkiri.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Long> {
    boolean existsByChatRoomAndInvitedMemberAndDelYn(
            ChatRoom chatRoom, Member invitedMember, String delYn);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatInvitation ci SET ci.delYn = 'Y' WHERE ci.invitedMember.id = :memberId")
    int softDeleteAllByInvitedMemberId(@Param("memberId") Long memberId);

}
