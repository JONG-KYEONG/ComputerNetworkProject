package com.project.server.socket.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRoomInfoMessage (
    MessageType messageType,
    String content,
    String sender,
    Long roomId,
    RoomInfoDto roomInfoDto,
    List<RoomUserDto> roomUserDtos
){}
