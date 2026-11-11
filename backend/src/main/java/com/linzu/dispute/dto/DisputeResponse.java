package com.linzu.dispute.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DisputeResponse(
        Long id,
        Long orderId,
        Long communityId,
        String communityName,
        Long initiatorId,
        String initiatorName,
        Long renterId,
        String renterName,
        Long ownerId,
        String ownerName,
        String reason,
        BigDecimal deposit,
        BigDecimal rent,
        String status,
        String stage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
