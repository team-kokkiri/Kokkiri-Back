package com.example.kokkiri.chat.repository;

import com.example.kokkiri.chat.domain.ChatParticipant;
import com.example.kokkiri.chat.domain.ChatRoom;
import com.example.kokkiri.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);
    Optional<ChatParticipant> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    List<ChatParticipant> findAllByMember(Member member);
    @Query("SELECT cp1.chatRoom FROM ChatParticipant cp1 JOIN ChatParticipant cp2 ON cp1.chatRoom.id = cp2.chatRoom.id WHERE cp1.member.id = :myId AND cp2.member.id = :otherMemberId AND cp1.chatRoom.isGroupChat = 'N'")
    Optional<ChatRoom> findExistingPrivateRoom(@Param("myId") Long myId, @Param("otherMemberId") Long otherMemberId);

    // 1:1 채팅에서 상대방 찾기
    @Query(value = """
    SELECT m.name
    FROM chat_participant cp
    JOIN member m ON cp.member_id = m.id
    WHERE cp.chat_room_id = :chatRoomId
      AND cp.member_id != :myMemberId
    LIMIT 1
    """, nativeQuery = true)
    String findOpponentNameByChatRoomId(@Param("chatRoomId") Long chatRoomId, @Param("myMemberId") Long myMemberId);

    Boolean existsByChatRoomAndMember(ChatRoom chatRoom, Member member);

}
