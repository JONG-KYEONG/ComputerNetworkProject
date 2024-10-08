package com.project.server.socket.dto;


import lombok.Builder;

@Builder
public record RoomUserDto(
        Long userId,
        boolean isCaptain,
        String roomNickname
) {
}
