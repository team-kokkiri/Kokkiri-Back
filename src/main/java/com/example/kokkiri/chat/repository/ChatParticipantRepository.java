package com.example.kokkiri.chat.repository;

import com.example.kokkiri.chat.domain.ChatParticipant;
import com.example.kokkiri.chat.domain.ChatRoom;
import com.example.kokkiri.chat.dto.MyChatListResDto;
import com.example.kokkiri.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);
    Page<ChatParticipant> findByChatRoom(ChatRoom chatRoom, Pageable pageable);
    Optional<ChatParticipant> findByChatRoomAndMember(ChatRoom chatRoom, Member member);
    List<ChatParticipant> findAllByMember(Member member);

    // member와 chatRoom을 fetch join으로 함께 가져오는 쿼리
    @Query("SELECT cp FROM ChatParticipant cp JOIN FETCH cp.chatRoom cr JOIN FETCH cp.member m WHERE cp.member = :member")
    List<ChatParticipant> findAllByMemberWithChatRoom(@Param("member") Member member); // 메서드 이름 변경 가능

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

    /**
     * 1. 정렬된 채팅방 ID 목록을 페이징하여 조회하는 쿼리
     * - 이 쿼리는 오직 '정렬'과 '페이징'에만 집중합니다.
     */
    @Query(value = """
    SELECT cp.chatRoom.id
    FROM ChatParticipant cp
    LEFT JOIN ChatMessage cm ON cm.chatRoom = cp.chatRoom
    WHERE cp.member = :member
    GROUP BY cp.chatRoom.id
    ORDER BY MAX(cm.createdTime) DESC NULLS LAST, cp.chatRoom.id DESC
    """,
            countQuery = """
    SELECT COUNT(DISTINCT cp.chatRoom.id)
    FROM ChatParticipant cp
    WHERE cp.member = :member
    """)
    Page<Long> findSortedChatRoomIdsByMember(@Param("member") Member member, Pageable pageable);

    /**
     * 2. 조회된 ID 목록을 기반으로 실제 DTO 데이터를 가져오는 쿼리
     * - 이 쿼리는 '데이터 조회'에만 집중하며, 정렬은 하지 않습니다.
     */
    @Query("""
    SELECT new com.example.kokkiri.chat.dto.MyChatListResDto(
        cp.chatRoom.id,
        CASE WHEN cp.chatRoom.isGroupChat = 'Y' THEN cp.chatRoom.name ELSE MIN(o.member.nickname) END,
        cp.chatRoom.isGroupChat,
        (SELECT COUNT(rs) FROM ReadStatus rs WHERE rs.chatRoom = cp.chatRoom AND rs.member = :member AND rs.isRead = false),
        (SELECT cm.content FROM ChatMessage cm WHERE cm.chatRoom = cp.chatRoom ORDER BY cm.createdTime DESC LIMIT 1),
        (SELECT cm.createdTime FROM ChatMessage cm WHERE cm.chatRoom = cp.chatRoom ORDER BY cm.createdTime DESC LIMIT 1)
    )
    FROM ChatParticipant cp
    LEFT JOIN ChatParticipant o ON o.chatRoom = cp.chatRoom AND o.member != :member
    WHERE cp.chatRoom.id IN :roomIds AND cp.member = :member
    AND (cp.chatRoom.isGroupChat = 'Y' OR o.member IS NOT NULL)
    GROUP BY cp.chatRoom.id, cp.chatRoom.name, cp.chatRoom.isGroupChat
    """)
    List<MyChatListResDto> findChatRoomDetailsByRoomIds(@Param("member") Member member, @Param("roomIds") List<Long> roomIds);

    // 채팅방 내에서 닉네임으로 멤버를 검색하는 페이징 메소드
    @Query("""
    SELECT cp FROM ChatParticipant cp
    WHERE cp.chatRoom = :chatRoom
      AND cp.member.nickname LIKE %:nickname%
    """)
    Page<ChatParticipant> findByChatRoomAndMemberNicknameContaining(
            @Param("chatRoom") ChatRoom chatRoom,
            @Param("nickname") String nickname,
            Pageable pageable
    );
}