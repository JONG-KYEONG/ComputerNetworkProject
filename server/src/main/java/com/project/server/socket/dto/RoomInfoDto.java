package com.project.server.socket.dto;

import lombok.Builder;

@Builder
public record RoomInfoDto(
        Long roomId,
        Integer userCount
) {
}
