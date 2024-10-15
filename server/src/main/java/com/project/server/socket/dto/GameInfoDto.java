package com.project.server.socket.dto;

import lombok.Builder;

@Builder
public record GameInfoDto(
        Long gameId,
        Integer userCount,
        String gameAnswerImage
) {
}
