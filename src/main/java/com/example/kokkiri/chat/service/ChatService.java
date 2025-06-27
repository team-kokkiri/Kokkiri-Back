package com.example.kokkiri.chat.service;


import com.example.kokkiri.chat.domain.*;
import com.example.kokkiri.chat.dto.ChatMessageDto;
import com.example.kokkiri.chat.dto.ChatRoomListResDto;
import com.example.kokkiri.chat.dto.MyChatListResDto;
import com.example.kokkiri.chat.repository.*;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.notification.domain.NotificationType;
import com.example.kokkiri.notification.repository.NotificationRepository;
import com.example.kokkiri.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MemberRepository memberRepository;
    private final ChatInvitationRepository chatInvitationRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatParticipantRepository chatParticipantRepository, ChatMessageRepository chatMessageRepository, ReadStatusRepository readStatusRepository, MemberRepository memberRepository, ChatInvitationRepository chatInvitationRepository, NotificationService notificationService, NotificationRepository notificationRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.memberRepository = memberRepository;
        this.chatInvitationRepository = chatInvitationRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    public ChatMessageDto saveMessage(Long roomId, ChatMessageDto chatMessageReqDto){
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("room cannot be found"));

        // 보낸 사람 조회
        Member sender = memberRepository.findByEmail(chatMessageReqDto.getSenderEmail()).orElseThrow(()-> new EntityNotFoundException("member cannot be found"));

        // 메세지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .member(sender)
                .content(chatMessageReqDto.getMessage())
                .build();
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // 사용자별로 읽음 여부 저장
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for(ChatParticipant c : chatParticipants){
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(chatRoom)
                    .member(c.getMember())
                    .chatMessage(chatMessage)
                    .isRead(c.getMember().equals(sender))   // 보낸 사람만 읽음 처리
                    .build();
            readStatusRepository.save(readStatus);

        }

        return ChatMessageDto.builder()
                .roomId(chatRoom.getId())
                .message(savedMessage.getContent())
                .senderEmail(sender.getEmail())
                .createdTime(savedMessage.getCreatedTime())
                .build();
    }

    public void sendChatNotification(Long roomId, String senderEmail, LocalDateTime actionCreatedAt){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("room cannot be found"));
        Member sender = memberRepository.findByEmail(senderEmail).orElseThrow(()-> new EntityNotFoundException("member cannot be found"));

        String url = "/my/chat/page";
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for(ChatParticipant c : chatParticipants) {
            if(!c.getMember().equals(sender)){
                notificationService.send(c.getMember(), NotificationType.CHAT, "new chat", url, null, actionCreatedAt);
            }
        }

    }


    public void createGroupRoom(String chatRoomName){
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(chatRoomName)
                .isGroupChat("Y")
                .build();

        chatRoomRepository.save(chatRoom);

        // 채팅 참여자로 개설자를 추가
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();

        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatRoomListResDto> getGroupChatRooms(){
        List<ChatRoom> chatRooms = chatRoomRepository.findByIsGroupChat("Y");
        List<ChatRoomListResDto> dtos = new ArrayList<>();
        for (ChatRoom r : chatRooms){
            ChatRoomListResDto dto = ChatRoomListResDto.builder()
                    .roomId(r.getId())
                    .roomName(r.getName())
                    .build();
            dtos.add(dto);
        }
        return dtos;
    }

    public void addParticipantToGroupChat(Long roomId){
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->new EntityNotFoundException("room cannot be found"));

        // member 조회
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        if (chatRoom.getIsGroupChat().equals("N")){
            throw new IllegalArgumentException("그룹 채팅이 아닙니다.");
        }
        // 이미 참여자인지 검증
        Optional<ChatParticipant> participant = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member);
        if (!participant.isPresent()){
            addParticipantToRoom(chatRoom, member);
        }
    }

    // ChatParticipant 객체 생성 후 저장
    public void addParticipantToRoom(ChatRoom chatRoom, Member member){
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .member(member)
                .build();
        chatParticipantRepository.save(chatParticipant);
    }

    public List<ChatMessageDto> getChatHistory(Long roomId){
        // 내가 해당 채팅방의 참여자가 아닐 경우 에러 발생
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->new EntityNotFoundException("room cannot be found"));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
//        List<ChatParticipant> chatParticipants = chatRoom.getChatParticipants();

        boolean check = false;
        for(ChatParticipant c : chatParticipants){
            if (c.getMember().equals(member)){
                check = true;
            }
        }
        if (!check) throw new IllegalArgumentException("속하지 않은 채팅방입니다.");

        // 특정 room에 대한 message 조회
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedTimeAsc(chatRoom);
        List<ChatMessageDto> chatMessageDtos = new ArrayList<>();
        for (ChatMessage c : chatMessages){
            ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                    .message(c.getContent())
                    .senderEmail(c.getMember().getEmail())
                    .createdTime(c.getCreatedTime())
                    .build();
            chatMessageDtos.add(chatMessageDto);
        }
        return chatMessageDtos;
    }

    public boolean isRoomParticipant(String email, Long roomId){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->new EntityNotFoundException("room cannot be found"));
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        for(ChatParticipant c : chatParticipants){
            if (c.getMember().equals(member)){
                return true;
            }
        }
        return false;
    }

    public void messageRead(Long roomId){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->new EntityNotFoundException("room cannot be found"));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        List<ReadStatus> readStatuses = readStatusRepository.findByChatRoomAndMember(chatRoom, member);
        for(ReadStatus r : readStatuses){
            r.updateIsRead(true);
        }
    }


    public Page<MyChatListResDto> getMyChatRooms(Pageable pageable) {
        // 1. 현재 사용자 정보 조회
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new EntityNotFoundException("member cannot be found"));

        // 2. [쿼리 1 실행] 정렬된 채팅방 ID 목록을 페이징하여 조회
        Page<Long> chatRoomIdsPage = chatParticipantRepository.findSortedChatRoomIdsByMember(member, pageable);
        List<Long> sortedRoomIds = chatRoomIdsPage.getContent();

        // 3. 조회된 채팅방 ID가 없으면 빈 페이지 반환
        if (sortedRoomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 4. [쿼리 2 실행] ID 목록을 사용하여 상세 정보 DTO 목록 조회 (이 결과는 순서가 보장되지 않음)
        List<MyChatListResDto> unsortedDetails = chatParticipantRepository.findChatRoomDetailsByRoomIds(member, sortedRoomIds);

        // 5. DTO 목록을 ID 기준으로 Map으로 변환 (순서 재정렬을 위함)
        Map<Long, MyChatListResDto> detailsMap = unsortedDetails.stream()
                .collect(Collectors.toMap(MyChatListResDto::getRoomId, dto -> dto));

        // 6. 원래 정렬된 ID 목록(sortedRoomIds)의 순서대로 DTO 목록을 재정렬
        List<MyChatListResDto> sortedDetails = sortedRoomIds.stream()
                .map(detailsMap::get)
                .collect(Collectors.toList());

        // 7. 최종적으로 Page 객체를 만들어 반환 (내용물, 페이징 정보, 전체 개수 포함)
        return new PageImpl<>(sortedDetails, pageable, chatRoomIdsPage.getTotalElements());
    }

    public void leaveGroupChatRoom(Long roomId){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->new EntityNotFoundException("room cannot be found"));
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        if (chatRoom.getIsGroupChat().equals("N")){
            throw new IllegalArgumentException("단체 채팅방이 아닙니다.");
        }

        ChatParticipant c = chatParticipantRepository.findByChatRoomAndMember(chatRoom, member).orElseThrow(()->new EntityNotFoundException("참여자를 찾을 수 없습니다."));
        chatParticipantRepository.delete(c);

        List<ChatParticipant> chatParticipants = chatParticipantRepository.findByChatRoom(chatRoom);
        if (chatParticipants.isEmpty()){
            chatRoomRepository.delete(chatRoom);
        }
    }

    public Long getOrCreatePrivateRoom(Long otherMemberId){
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));
        Member otherMember = memberRepository.findById(otherMemberId).orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        // 자기 자신과의 채팅방 생성을 방지
        if (member.getId().equals(otherMember.getId())) {
            throw new IllegalArgumentException("Cannot create a private chat room with yourself.");
        }

        // 나와 상대방이 일대일 채팅에 이미 참석하고 있다면 해당 roomId 리턴
        Optional<ChatRoom> chatRoom = chatParticipantRepository.findExistingPrivateRoom(member.getId(), otherMember.getId());
        if (chatRoom.isPresent()){
            return chatRoom.get().getId();
        }

        // 만약 일대일 채팅방이 없을 경우 기존 채팅방 개설
        ChatRoom newRoom = ChatRoom.builder()
                .isGroupChat("N")
                .name(member.getNickname() + "-" + otherMember.getNickname())
                .build();
        chatRoomRepository.save(newRoom);

        // 두 사람 모두 참여자로 새롭게 추가
        addParticipantToRoom(newRoom, member);
        addParticipantToRoom(newRoom, otherMember);

        return newRoom.getId();
    }

    public void inviteMember(Long roomId, Long memberId){
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()->new EntityNotFoundException("room cannot be found"));

        if (chatRoom.getIsGroupChat().equals("N")){
            throw new IllegalArgumentException("단체 채팅방이 아닙니다.");
        }

        Member invitedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일의 유저를 찾을 수 없습니다."));

        boolean alreadyExists = chatParticipantRepository.existsByChatRoomAndMember(chatRoom, invitedMember);
        if (alreadyExists) {
            throw new IllegalStateException("이미 참여중인 사용자입니다.");
        }

        boolean alreadyInvited = chatInvitationRepository.existsByChatRoomAndInvitedMemberAndDelYn(chatRoom, invitedMember, "N");
        if (alreadyInvited) {
            throw new IllegalStateException("이미 초대장을 보낸 유저입니다.");
        }

        Member inviter = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));

        ChatInvitation invitation = ChatInvitation.builder()
                .invitedMember(invitedMember)
                .inviterId(inviter.getId())
                .chatRoom(chatRoom)
                .build();

        chatInvitationRepository.save(invitation);

        String content = inviter.getNickname() + "님이 <" + chatRoom.getName() + "> 그룹 채팅에 초대하였습니다.";
        String url = "/chatPage/" + chatRoom.getId();
        notificationService.send(invitedMember, NotificationType.INVITATION, content, url, invitation.getId(), invitation.getCreatedTime());
    }

    public Long acceptInvitation(Long invitationId) {
        ChatInvitation invitation = chatInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("초대를 찾을 수 없습니다."));

        Member invitedMember = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(() -> new EntityNotFoundException("member cannot be found"));


        boolean alreadyExists = chatParticipantRepository.existsByChatRoomAndMember(invitation.getChatRoom(), invitedMember);
        if (alreadyExists) {
            throw new IllegalStateException("이미 참여중인 채팅방입니다.");
        }

        ChatParticipant newParticipant = ChatParticipant.builder()
                .chatRoom(invitation.getChatRoom())
                .member(invitedMember)
                .build();

        chatParticipantRepository.save(newParticipant);
        invitation.delete();
        // 5. 알림 처리: isRead = 'Y', delYn = 'Y'로 변경
        // invitationId를 사용하여 관련 알림을 찾음
        notificationService.findByInvitationId(invitationId).ifPresent(notification -> {
//            notification.updateIsRead();
            notification.delete();
        });

        Long joinedRoomId = invitation.getChatRoom().getId();

        return joinedRoomId;
    }

    public void rejectInvitation(Long invitationId) {
        ChatInvitation invitation = chatInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("초대를 찾을 수 없습니다."));

        invitation.delete();
        notificationService.findByInvitationId(invitationId).ifPresent(notification -> {
//            notification.updateIsRead();
            notification.delete();
        });
    }
}
