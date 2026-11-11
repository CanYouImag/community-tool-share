package com.linzu.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FreezeRequest(
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    BigDecimal amount,

    @Size(max = 32, message = "关联类型过长") String refType,
    Long refId,
    @Size(max = 255, message = "备注最多 255 个字符") String remark
) {}
