package com.project.server.socket.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatGameInfoMessage(
    MessageType messageType,
    String content,
    String sender,
    Long gameId,
    GameInfoDto gameInfoDto,
    List<GameUserDto> gameUserDtos
){}
