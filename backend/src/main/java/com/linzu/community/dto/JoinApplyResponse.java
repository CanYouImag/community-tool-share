package com.linzu.community.dto;

import java.time.LocalDateTime;

public record JoinApplyResponse(
        Long id,
        Long userId,
        String nickname,
        String avatarUrl,
        String applyReason,
        String status,
        LocalDateTime createdAt
) {
}
