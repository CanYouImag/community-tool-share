package com.linzu.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 平台审核物品（UC-C6）
 */
public record ItemReviewRequest(
    @NotNull(message = "审核结论不能为空")
    Boolean approved,

    String reason
) {}
