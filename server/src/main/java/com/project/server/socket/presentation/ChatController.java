package com.project.server.socket.presentation;

import com.project.server.game.service.GameService;
import com.project.server.socket.application.ChatService;
import com.project.server.socket.dto.*;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
public class ChatController {
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;
    private final GameService gameService;

    @MessageMapping("/chat.sendMessage")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {   // 채팅
        String destination = "/topic/public/"+chatMessage.gameId();
        messagingTemplate.convertAndSend(destination, chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    public ChatGameInfoMessage addUser(@Payload ChatMessage chatMessage,
                                       SimpMessageHeaderAccessor headerAccessor) {   // 게임 입장
        Long userId = gameService.enterGame(chatMessage.gameId(), chatMessage.sender());
        GameInfoDto gameInfoDto = gameService.getGameInfoDto(chatMessage.gameId());
        List<GameUserDto> gameUserDtos = gameService.getGameUsers(chatMessage.gameId());

        String sender = chatMessage.sender();
        headerAccessor.getSessionAttributes().put("userId", userId);
        headerAccessor.getSessionAttributes().put("username", sender);
        headerAccessor.getSessionAttributes().put("gameId", chatMessage.gameId());

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
    public ChatGameInfoMessage startGame(@Payload ChatMessage chatMessage) {  // 게임 시작
        String sender = chatMessage.sender();

        gameService.gameStart(chatMessage.gameId());
        GameInfoDto gameInfoDto = gameService.changeTurn(chatMessage.gameId());
        List<GameUserDto> gameUserDtos = gameService.getGameUsers(chatMessage.gameId());

        ChatGameInfoMessage chatgameInfoMessage = ChatGameInfoMessage.builder()
                .messageType(MessageType.START)
                .gameId(chatMessage.gameId())
                .content("*** 게임 시작! 사진을 보고 누구 인지 맞춰 보세요! ***")
                .sender(sender)
                .gameInfoDto(gameInfoDto)
                .gameUserDtos(gameUserDtos)
                .build();

        messagingTemplate.convertAndSend("/topic/public/" + chatMessage.gameId(), chatgameInfoMessage);

        return chatgameInfoMessage;
    }

    @MessageMapping("/chat.correctAnswer")
    public ChatGameInfoMessage correctAnswer(@Payload ChatMessage chatMessage) {  // 정답 맞추기
        String sender = chatMessage.sender();
        String destination = "/topic/public/"+chatMessage.gameId();

        if(gameService.isCorrect(chatMessage.content(), chatMessage.gameId(), chatMessage.senderId())){  // 정답이라면 추가 정
            String answer = gameService.getAnswer(chatMessage.gameId());
            GameInfoDto gameInfoDto = gameService.changeTurn(chatMessage.gameId());
            List<GameUserDto> gameUserDtos = gameService.getGameUsers(chatMessage.gameId());

            ChatGameInfoMessage chatgameInfoMessage = ChatGameInfoMessage.builder()
                    .messageType(MessageType.ANSWER)
                    .gameId(chatMessage.gameId())
                    .content(sender+"님 ** "+ answer +" ** 정답!")
                    .sender(sender)
                    .gameInfoDto(gameInfoDto)
                    .gameUserDtos(gameUserDtos)
                    .build();

            messagingTemplate.convertAndSend(destination, chatgameInfoMessage);

            if(gameService.gameEnd(chatMessage.gameId())){
                String result = gameService.getResult(chatMessage.gameId());
                ChatGameInfoMessage chatEndMessage = ChatGameInfoMessage.builder()
                        .messageType(MessageType.END)
                        .gameId(chatMessage.gameId())
                        .content("*** 게임이 종료 되었습니다! *** \n" + result)
                        .sender(sender)
                        .gameInfoDto(gameInfoDto)
                        .gameUserDtos(gameUserDtos)
                        .build();

                messagingTemplate.convertAndSend(destination, chatEndMessage);
            }
        }
        else{  // 정답이 아니라면 그냥 채팅으로
            ChatMessage newChatMessage = ChatMessage.builder()
                    .messageType(MessageType.CHAT)
                    .content(chatMessage.content())
                    .gameId(chatMessage.gameId())
                    .sender(sender)
                    .senderId(chatMessage.senderId())
                    .build();

            messagingTemplate.convertAndSend(destination, newChatMessage);
        }

        return null;
    }

    @PostMapping("/api/create/game")
    public Long createGame(){
        return gameService.createGame();
    }

    @GetMapping("/api/game")
    public Long getGame(){
        return gameService.getGame();
    }
}
