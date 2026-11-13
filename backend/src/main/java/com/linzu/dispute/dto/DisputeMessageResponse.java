package com.linzu.dispute.dto;

import java.time.LocalDateTime;

public record DisputeMessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String senderRole,
        String content,
        LocalDateTime createdAt
) {
}
