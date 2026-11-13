package com.linzu.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单响应。pickupCode 仅在待取物状态对租客与出租人可见。
 */
public record OrderResponse(
    Long id,
    String orderNo,
    Long itemId,
    String itemTitle,
    String itemImage,
    Long lessorId,
    Long renterId,
    LocalDate startDate,
    LocalDate endDate,
    Integer days,
    BigDecimal rentAmount,
    BigDecimal depositAmount,
    BigDecimal commissionAmount,
    String status,
    String pickupCode,
    String cancelReason,
    LocalDateTime paidAt,
    LocalDateTime acceptedAt,
    LocalDateTime pickupAt,
    LocalDateTime returnAt,
    LocalDateTime completedAt,
    LocalDateTime cancelledAt,
    LocalDateTime createdAt
) {}
