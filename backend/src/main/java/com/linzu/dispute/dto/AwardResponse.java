package com.linzu.dispute.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AwardResponse(
        Long id,
        Long orderId,
        String applicant,
        String counterpart,
        BigDecimal deposit,
        String type,
        BigDecimal toLender,
        BigDecimal toBorrower,
        BigDecimal rentRelief,
        String reason,
        LocalDateTime time,
        String arbitrator
) {
}
