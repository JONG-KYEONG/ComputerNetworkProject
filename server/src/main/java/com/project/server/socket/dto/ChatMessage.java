package com.project.server.socket.dto;

import lombok.Builder;

@Builder
public record ChatMessage (
    MessageType messageType,
    String content,
    Long senderId,
    String sender,
    Long gameId
){}
