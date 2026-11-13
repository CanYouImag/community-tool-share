package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record DisputeCreateRequest(
        @NotNull(message = "订单不能为空")
        Long orderId,

        @NotNull(message = "社区不能为空")
        Long communityId,

        @NotNull(message = "租客不能为空")
        Long renterId,

        @NotNull(message = "出租者不能为空")
        Long ownerId,

        @NotNull(message = "扣留金额不能为空")
        BigDecimal withholdAmount,

        @NotBlank(message = "纠纷理由不能为空")
        @Size(max = 500, message = "纠纷理由最多 500 个字符")
        String reason,

        BigDecimal depositAmount,

        BigDecimal rentAmount,

        List<@Size(max = 255) String> evidenceUrls
) {
}
