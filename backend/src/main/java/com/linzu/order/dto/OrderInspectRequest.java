package com.linzu.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 验收（UC-D6）：pass=true 验收通过并结算；pass=false 进入纠纷。
 */
public record OrderInspectRequest(
    @NotNull(message = "验收结论不能为空")
    Boolean pass,

    String reason
) {}
