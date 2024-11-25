package com.project.server.socket.presentation;

import com.project.server.game.service.GameService;
import com.project.server.socket.dto.ChatGameInfoMessage;
import com.project.server.socket.dto.MessageType;
import com.project.server.socket.dto.GameInfoDto;
import com.project.server.socket.dto.GameUserDto;
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
    private final GameService gameService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    @EventListener
    @Transactional
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) throws InterruptedException {
        // 소켓 연결 해제 시 처리
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("username");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        Long gameId = (Long) headerAccessor.getSessionAttributes().get("gameId");
        String destination = "/topic/public/"+gameId;

        GameInfoDto gameInfoDto = gameService.leaveGame(gameId, userId);

        if(gameInfoDto!=null){
            List<GameUserDto> gameUserDtos = gameService.getGameUsers(gameId);

            ChatGameInfoMessage chatGameInfoMessage = ChatGameInfoMessage.builder()
                    .messageType(MessageType.LEAVE)
                    .gameId(gameId)
                    .content(username + " 님이 퇴장하셨습니다.")
                    .sender(username)
                    .gameInfoDto(gameInfoDto)
                    .gameUserDtos(gameUserDtos)
                    .build();

            messagingTemplate.convertAndSend(destination, chatGameInfoMessage);
        }
    }

}