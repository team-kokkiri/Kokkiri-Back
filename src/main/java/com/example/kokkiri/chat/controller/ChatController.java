package com.example.kokkiri.chat.controller;

import com.example.kokkiri.chat.dto.ChatMemberDto;
import com.example.kokkiri.chat.dto.ChatMessageDto;
import com.example.kokkiri.chat.dto.ChatRoomListResDto;
import com.example.kokkiri.chat.dto.MyChatListResDto;
import com.example.kokkiri.chat.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /*
    * 그룹 채팅 관련 메서드
    * */
    // 그룹 채팅방 개설
    @PostMapping("/room/group/create")
    public ResponseEntity<?> createGroupRoom(@RequestParam String roomName){
        chatService.createGroupRoom(roomName);
        return ResponseEntity.ok().build();
    }

    // 그룹 채팅 목록 조회
    @GetMapping("/room/group/list")
    public ResponseEntity<?> getGroupChatRooms(){
        List<ChatRoomListResDto> chatRooms = chatService.getGroupChatRooms();
        return new ResponseEntity<>(chatRooms, HttpStatus.OK);
    }

    // 그룹 채팅방 참여
    @PostMapping("/room/group/{roomId}/join")
    public ResponseEntity<?> joinGroupChatRoom(@PathVariable Long roomId){
        chatService.addParticipantToGroupChat(roomId);
        return ResponseEntity.ok().build();
    }

    // 이전 메세지 조회
    @GetMapping("/history/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long roomId){
        List<ChatMessageDto> chatMessageDtos = chatService.getChatHistory(roomId);
        return new ResponseEntity<>(chatMessageDtos, HttpStatus.OK);
    }

    // 채팅 메세지 읽음 처리
    @PostMapping("/room/{roomId}/read")
    public ResponseEntity<?> messageRead(@PathVariable Long roomId){
        chatService.messageRead(roomId);
        return ResponseEntity.ok().build();
    }

    // 내 채팅방 목록 조회: roomId, roomName, 그룹채팅여부, 메세지 읽음 개수
    @GetMapping("/myRooms")
    public ResponseEntity<?> getMyChatRooms(@PageableDefault(page = 0, size = 20) Pageable pageable) {

        // ChatService는 이제 Page 객체를 반환합니다.
        Page<MyChatListResDto> myChatRoomsPage = chatService.getMyChatRooms(pageable);

        // Page 객체 자체를 반환하면, Spring이 알아서 JSON으로 변환해줍니다.
        // 이 JSON에는 채팅 목록(content) 외에 총 페이지 수, 현재 페이지 번호 등 유용한 정보가 모두 포함됩니다.
        return new ResponseEntity<>(myChatRoomsPage, HttpStatus.OK);
    }

    // 채팅방 나가기
    @DeleteMapping("/room/group/{roomId}/leave")
    public ResponseEntity<?> leaveGroupChatRoom(@PathVariable Long roomId){
        chatService.leaveGroupChatRoom(roomId);
        return ResponseEntity.ok().build();
    }

    // 개인 채팅방 개설 또는 기존 roomId 리턴
    @PostMapping("/room/private/create")
    public ResponseEntity<?> getOrCreatePrivateRoom(@RequestParam Long otherMemberId){
        Long roomId = chatService.getOrCreatePrivateRoom(otherMemberId);
        return new ResponseEntity<>(roomId, HttpStatus.OK);
    }

    // 채팅방 초대
    @PostMapping("/room/group/{roomId}/invite")
    public ResponseEntity<?> inviteMember(@PathVariable Long roomId, @RequestParam Long memberId){
        chatService.inviteMember(roomId, memberId);
        return ResponseEntity.ok().build();
    }

    // 그룹 채팅 초대 수락
    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Long invitationId){
        Long joinedRoomId = chatService.acceptInvitation(invitationId);
        Map<String, Long> response = new HashMap<>();
        response.put("roomId", joinedRoomId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 그룹 채팅 초대 거절
    @PostMapping("/invitations/{invitationId}/reject")
    public ResponseEntity<?> rejectInvitation(@PathVariable Long invitationId){
        chatService.rejectInvitation(invitationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 채팅방의 멤버 목록을 조회하는 API
     * @param roomId 채팅방 ID
     * @return 채팅방 멤버 정보 리스트 (memberId, nickname, avatarUrl)
     */
    @GetMapping("/room/{roomId}/members")
    public ResponseEntity<?> getChatRoomMembers(
            @PathVariable Long roomId,
            @PageableDefault(size = 20) Pageable pageable) throws AccessDeniedException {
        Page<ChatMemberDto> members = chatService.getChatRoomMembers(roomId, pageable);
        return ResponseEntity.ok(members);
    }



}
