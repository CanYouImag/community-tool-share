package com.linzu.platform.dto;

import java.time.LocalDateTime;

public record CommunityAdminResponse(
        Long id,
        String name,
        String intro,
        String coverUrl,
        Boolean autoApprove,
        Integer memberCount,
        Integer itemCount,
        String status,
        Long adminId,
        String adminName,
        LocalDateTime createdAt
) {
}
