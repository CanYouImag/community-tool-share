package com.linzu.dispute.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DisputeDetailResponse(
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
        String mediationConclusion,
        String escalateNote,
        BigDecimal withholdAmount,
        String renterNote,
        List<DisputeEvidenceResponse> evidences,
        List<DisputeMessageResponse> messages,
        AwardResponse award,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
