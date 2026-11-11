package com.linzu.dispute.dto;

import java.time.LocalDateTime;

public record DisputeEvidenceResponse(
        Long id,
        String imageUrl,
        String evidenceType,
        Long uploadedBy,
        LocalDateTime createdAt
) {
}
