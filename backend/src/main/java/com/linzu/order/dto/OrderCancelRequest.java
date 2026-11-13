package com.linzu.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 订单取消（UC-D8）
 */
public record OrderCancelRequest(
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 255, message = "取消原因最长 255 字")
    String reason
) {}
