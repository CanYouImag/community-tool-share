package com.linzu.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 出租人验码取物（UC-D4）
 */
public record OrderPickupRequest(
    @NotBlank(message = "取物码不能为空")
    String code
) {}
