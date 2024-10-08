package com.project.server.socket.presentation;

import com.project.server.room.service.RoomService;
import com.project.server.socket.application.ChatService;
import com.project.server.socket.dto.*;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
public class ChatController {
    private final SimpMessageSendingOperations messagingTemplate;
//    private final ChatService chatService;
    private final RoomService roomService;

    @MessageMapping("/chat.sendMessage")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        String destination = "/topic/public/"+chatMessage.getRoomId();
        messagingTemplate.convertAndSend(destination, chatMessage);
        return chatMessage;
    }


    @MessageMapping("/chat.addUser")
    public ChatRoomInfoMessage addUser(@Payload ChatMessage chatMessage,
                                       SimpMessageHeaderAccessor headerAccessor) {
        String sender = chatMessage.getSender();
        headerAccessor.getSessionAttributes().put("userId", chatMessage.getSenderId());
        headerAccessor.getSessionAttributes().put("username", sender);
        headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());

        RoomInfoDto roomInfoDto = roomService.enterRoom(chatMessage.getRoomId(), chatMessage.getSender());
        List<RoomUserDto> roomUserDtos = roomService.getRoomUsers(chatMessage.roomId());

        ChatRoomInfoMessage chatRoomInfoMessage = ChatRoomInfoMessage.builder()
                .messageType(MessageType.JOIN)
                .roomId(chatMessage.roomId())
                .content(sender + " 님이 입장하셨습니다.")
                .sender(sender)
                .roomInfoDto(roomInfoDto)
                .roomUserDtos(roomUserDtos)
                .build();

        messagingTemplate.convertAndSend("/topic/public/" + chatMessage.getRoomId(), chatRoomInfoMessage);

        return chatRoomInfoMessage;
    }
}
