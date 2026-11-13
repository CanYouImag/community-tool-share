package com.linzu.order.dto;

import java.time.LocalDateTime;

public record OrderEvaluationResponse(
    Long id,
    Long orderId,
    Long itemId,
    Long evaluatorId,
    String evaluatorNickname,
    Long targetUserId,
    Integer rating,
    String content,
    String imageUrls,
    LocalDateTime createdAt
) {}
