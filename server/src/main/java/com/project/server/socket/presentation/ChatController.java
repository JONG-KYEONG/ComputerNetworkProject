package com.project.server.socket.presentation;

import com.project.server.game.service.GameService;
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
    private final ChatService chatService;
    private final GameService gameService;

    @MessageMapping("/chat.sendMessage")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        String destination = "/topic/public/"+chatMessage.gameId();
        messagingTemplate.convertAndSend(destination, chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    public ChatGameInfoMessage addUser(@Payload ChatMessage chatMessage,
                                       SimpMessageHeaderAccessor headerAccessor) {
        String sender = chatMessage.sender();
        headerAccessor.getSessionAttributes().put("userId", chatMessage.senderId());
        headerAccessor.getSessionAttributes().put("username", sender);
        headerAccessor.getSessionAttributes().put("gameId", chatMessage.gameId());

        GameInfoDto gameInfoDto = gameService.enterGame(chatMessage.gameId(), chatMessage.sender());
        List<GameUserDto> gameUserDtos = gameService.getGameUsers(chatMessage.gameId());

        ChatGameInfoMessage chatGameInfoMessage = ChatGameInfoMessage.builder()
                .messageType(MessageType.JOIN)
                .gameId(chatMessage.gameId())
                .content(sender + " 님이 입장하셨습니다.")
                .sender(sender)
                .gameInfoDto(gameInfoDto)
                .gameUserDtos(gameUserDtos)
                .build();

        messagingTemplate.convertAndSend("/topic/public/" + chatMessage.gameId(), chatGameInfoMessage);

        return chatGameInfoMessage;
    }

    @MessageMapping("/chat.startGame")
    public ChatGameInfoMessage startGame(@Payload ChatMessage chatMessage,
                                         SimpMessageHeaderAccessor headerAccessor) {
        String sender = chatMessage.sender();

        GameInfoDto gameInfoDto = gameService.enterGame(chatMessage.gameId(), chatMessage.sender());
        List<GameUserDto> gameUserDtos = gameService.getGameUsers(chatMessage.gameId());

        ChatGameInfoMessage chatgameInfoMessage = ChatGameInfoMessage.builder()
                .messageType(MessageType.START)
                .gameId(chatMessage.gameId())
                .content("*** 게임 시작! *** \n 사진을 보고 누구 인지 맞춰 보세요! ")
                .sender(sender)
                .gameInfoDto(gameInfoDto)
                .gameUserDtos(gameUserDtos)
                .build();

        messagingTemplate.convertAndSend("/topic/public/" + chatMessage.gameId(), chatgameInfoMessage);

        return chatgameInfoMessage;
    }
}
