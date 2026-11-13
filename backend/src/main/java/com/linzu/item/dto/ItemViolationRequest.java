package com.linzu.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 平台违规下架（UC-C7）
 */
public record ItemViolationRequest(
    @NotNull(message = "操作不能为空")
    Boolean violationOff,

    String reason
) {}
