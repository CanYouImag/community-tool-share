package com.linzu.user.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String phone,
        String nickname,
        String avatarUrl,
        String bio,
        String role,
        String status,
        Integer creditScore,
        String realNameStatus,
        String realName,
        String idCardMasked,
        LocalDateTime createdAt
) {
}
