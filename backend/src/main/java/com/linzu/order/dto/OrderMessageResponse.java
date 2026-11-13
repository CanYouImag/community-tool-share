package com.linzu.order.dto;

import java.time.LocalDateTime;

/**
 * 订单留言响应（UC-D10）
 */
public record OrderMessageResponse(
    Long id,
    Long orderId,
    Long senderId,
    String senderNickname,
    String senderAvatar,
    String content,
    LocalDateTime createdAt
) {}
