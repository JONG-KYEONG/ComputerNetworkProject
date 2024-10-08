package com.project.server.socket.dto;


import lombok.Builder;

@Builder
public record GameUserDto(
        Long userId,
        boolean isCaptain,
        String gameNickname,
        Integer gameScore
) {
}
