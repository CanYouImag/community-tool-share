package com.linzu.platform.dto;

import java.math.BigDecimal;

public record ConfigUpdateRequest(
        BigDecimal commissionRate,
        Boolean communityAutoApprove,
        Boolean itemAuditEnabled
) {
}
