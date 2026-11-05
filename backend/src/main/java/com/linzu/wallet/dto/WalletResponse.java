package com.linzu.wallet.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        BigDecimal freezeBalance,
        BigDecimal totalIncome,
        BigDecimal recentRentSpent
) {
}
