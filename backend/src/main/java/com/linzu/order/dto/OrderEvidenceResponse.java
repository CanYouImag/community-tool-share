package com.linzu.order.dto;

import java.time.LocalDateTime;

public record OrderEvidenceResponse(
    Long id,
    Long orderId,
    String stage,
    String imageUrl,
    Long uploadedBy,
    LocalDateTime createdAt
) {}
