package com.linzu.community.dto;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {
}
