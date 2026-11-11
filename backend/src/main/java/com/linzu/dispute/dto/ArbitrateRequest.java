package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ArbitrateRequest(
        @NotBlank(message = "裁决类型不能为空")
        String verdictType,

        BigDecimal deductAmount,

        BigDecimal rentDiscount,

        @NotBlank(message = "裁决理由不能为空")
        @Size(max = 2000, message = "裁决理由最多 2000 个字符")
        String awardReason
) {
}
