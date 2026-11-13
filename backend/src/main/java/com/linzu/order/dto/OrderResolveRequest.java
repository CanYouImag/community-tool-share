package com.linzu.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 平台调解纠纷：outcome 取 COMPLETED（判出租方履约，正常结算）或
 * CANCELLED（判租方胜诉，租金押金全额退还）。
 * outcome=COMPLETED 时可指定 deductAmount 从押金中扣除赔偿给出租人。
 */
public record OrderResolveRequest(
    @NotNull(message = "调解结论不能为空")
    String outcome,

    java.math.BigDecimal deductAmount,

    @NotBlank(message = "调解说明不能为空")
    String reason
) {}
