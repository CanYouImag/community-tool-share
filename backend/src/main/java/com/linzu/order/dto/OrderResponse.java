package com.linzu.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long itemId,
        String itemTitle,
        Long communityId,
        Long renterId,
        Long ownerId,
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        String status,
        LocalDateTime createdAt
) {
}
