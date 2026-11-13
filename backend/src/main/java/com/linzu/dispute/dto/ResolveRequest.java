package com.linzu.dispute.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ResolveRequest(
        @NotBlank(message = "调解结论不能为空")
        @Size(max = 500, message = "调解结论最多 500 个字符")
        String conclusion,

        @DecimalMin(value = "0", message = "调解扣除金额不能为负")
        BigDecimal deductAmount
) {
}
