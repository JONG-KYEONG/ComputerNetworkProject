package com.project.server.socket.presentation;

import com.project.server.room.domain.Room;
import com.project.server.room.domain.RoomUser;
import com.project.server.room.service.RoomService;
import com.project.server.socket.dto.ChatRoomInfoMessage;
import com.project.server.socket.dto.MessageType;
import com.project.server.socket.dto.RoomInfoDto;
import com.project.server.socket.dto.RoomUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Objects;


@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final RoomService roomService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    @Transactional
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) throws InterruptedException {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("username");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        Long roomId = (Long) headerAccessor.getSessionAttributes().get("roomId");
        String destination = "/topic/public/"+roomId;

        RoomInfoDto roomInfoDto = roomService.leaveRoom(roomId, userId);
        List<RoomUserDto> roomUserDtos = roomService.getRoomUsers(roomId);

        ChatRoomInfoMessage chatRoomInfoMessage = ChatRoomInfoMessage.builder()
                .messageType(MessageType.LEAVE)
                .roomId(roomId)
                .content(username + " 님이 퇴장하셨습니다.")
                .sender(username)
                .roomInfoDto(roomInfoDto)
                .roomUserDtos(roomUserDtos)
                .build();

        messagingTemplate.convertAndSend(destination, chatRoomInfoMessage);
    }

}