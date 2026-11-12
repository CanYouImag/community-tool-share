package com.linzu.wallet.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 管理员调整余额：amount 为正数充值，负数扣减。
 */
public record AdjustBalanceRequest(
    @NotNull(message = "目标用户不能为空") Long userId,
    @NotNull(message = "调整金额不能为空")
    @Digits(integer = 10, fraction = 2, message = "调整金额最多保留两位小数")
    BigDecimal amount,
    @Size(max = 255, message = "备注最多 255 个字符") String remark
) {}
