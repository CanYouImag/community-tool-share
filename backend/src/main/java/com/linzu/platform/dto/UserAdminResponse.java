package com.linzu.platform.dto;

import java.time.LocalDateTime;

public record UserAdminResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String role,
        String status,
        Integer creditScore,
        LocalDateTime createdAt
) {
}
