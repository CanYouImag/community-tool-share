package com.linzu.order.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 创建订单（UC-D1）：按租期计算租金，押金取物品配置。
 */
public record CreateOrderRequest(
    @NotNull(message = "物品不能为空")
    Long itemId,

    @NotNull(message = "开始日期不能为空")
    LocalDate startDate,

    @NotNull(message = "结束日期不能为空")
    LocalDate endDate
) {}
