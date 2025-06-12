package com.example.kokkiri.chat.controller;

import com.example.chatserver.chat.dto.ChatMessageDto;
import com.example.chatserver.chat.service.ChatService;
import com.example.chatserver.chat.service.RedisPubSubService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class StompController {

//    방법 1. MessageMapping(수신)과 SendTo(topic에 메세지 전송)를 한번에 처리
//    @MessageMapping("/{roomId}")    // 클라이언트에서 특정 publish/roomId 형태로 메세지를 발행시 MessageMapping 수신
//    @SendTo("/topic/{roomId}")      // 해당 roomId의 메세지를 발행하여 구독중인 클라이언트에게 메세지 전송
//    // @DestinationVariable : @MessageMapping 어노테이션으로 정의된 WebSocket Controller 내에서만 사용
//    public String sendMessage(@DestinationVariable Long roomId, String message){
//        System.out.println(message);
//
//        return message;
//    }


//    방법 2. MessageMapping 어노테이션만 활용
//    Send는 로직 내부에 별도로 직접 구현
//    Redis Pub/sub과 같은 기능을 연동할 때 @SendTo 사용시 소스코드의 유연성 떨어짐
    private final SimpMessageSendingOperations messsageTemplate;
    private final ChatService chatService;
    private final RedisPubSubService pubSubService;
    public StompController(SimpMessageSendingOperations messsageTemplate, ChatService chatService, RedisPubSubService pubSubService) {
        this.messsageTemplate = messsageTemplate;
        this.chatService = chatService;
        this.pubSubService = pubSubService;
    }

    @MessageMapping("/{roomId}")    // 클라이언트에서 특정 publish/roomId 형태로 메세지를 발행시 MessageMapping 수신
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto chatMessageReqDto) throws JsonProcessingException {
        System.out.println(chatMessageReqDto.getMessage());
        chatService.saveMessage(roomId, chatMessageReqDto);
//        messsageTemplate.convertAndSend("/topic/"+roomId, chatMessageReqDto);

        chatMessageReqDto.setRoomId(roomId);
        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(chatMessageReqDto);
        pubSubService.publish("chat", message);
    }
}
