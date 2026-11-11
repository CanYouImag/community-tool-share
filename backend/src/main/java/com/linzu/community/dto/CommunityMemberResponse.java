package com.linzu.community.dto;

import java.time.LocalDateTime;

public record CommunityMemberResponse(
        Long userId,
        String nickname,
        String avatarUrl,
        String role,
        LocalDateTime joinedAt
) {
}
