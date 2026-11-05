package com.linzu.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long id,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String refType,
        Long refId,
        String remark,
        LocalDateTime time
) {
}
