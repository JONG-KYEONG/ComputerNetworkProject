package com.project.server.socket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public record ChatMessage (
    MessageType messageType,
    String content,
    Long senderId,
    String sender,
    Long roomId
){}
