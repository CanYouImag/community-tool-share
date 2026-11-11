package com.linzu.wallet.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.wallet.dto.WalletResponse;
import com.linzu.wallet.dto.WalletTransactionResponse;
import com.linzu.wallet.entity.Wallet;
import com.linzu.wallet.entity.WalletTransaction;
import com.linzu.wallet.mapper.WalletMapper;
import com.linzu.wallet.mapper.WalletTransactionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    public WalletResponse getWallet(Long userId) {
        Wallet wallet = getOrCreate(userId);
        return new WalletResponse(
            wallet.getId(),
            wallet.getBalance(),
            wallet.getFrozenBalance(),
            wallet.getTotalIncome(),
            recentRentSpent(userId)
        );
    }

    public PageResult<WalletTransactionResponse> getTransactions(
        Long userId,
        long pageNo,
        long pageSize,
        String type
    ) {
        long safePage = Math.max(1, pageNo);
        long safeSize = Math.min(100, Math.max(1, pageSize));

        Page<WalletTransaction> page = new Page<>(safePage, safeSize);
        var wrapper = Wrappers.<WalletTransaction>lambdaQuery()
            .eq(WalletTransaction::getUserId, userId)
            .eq(StringUtils.hasText(type), WalletTransaction::getType, type)
            .orderByDesc(WalletTransaction::getCreatedAt)
            .orderByDesc(WalletTransaction::getId);

        Page<WalletTransaction> result = walletTransactionMapper.selectPage(page, wrapper);
        List<WalletTransactionResponse> records = result
            .getRecords()
            .stream()
            .map(this::toResponse)
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 冻结可用余额，用于订单押金或租金预授权。
     */
    @Transactional
    public WalletResponse freeze(
        Long userId,
        BigDecimal amount,
        String refType,
        Long refId,
        String remark
    ) {
        BigDecimal value = requirePositive(amount);
        Wallet wallet = getOrCreate(userId);
        if (wallet.getBalance().compareTo(value) < 0) {
            throw new BusinessException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
        }

        wallet.setBalance(wallet.getBalance().subtract(value));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(value));
        updateWallet(wallet);
        record(
            userId,
            WalletTransactionType.FREEZE,
            value.negate(),
            wallet.getBalance(),
            refType,
            refId,
            remark
        );
        return getWallet(userId);
    }

    /**
     * 解冻金额，回到可用余额。
     */
    @Transactional
    public WalletResponse unfreeze(
        Long userId,
        BigDecimal amount,
        String refType,
        Long refId,
        String remark
    ) {
        BigDecimal value = requirePositive(amount);
        Wallet wallet = getOrCreate(userId);
        if (wallet.getFrozenBalance().compareTo(value) < 0) {
            throw new BusinessException(ErrorCode.WALLET_FROZEN_NOT_ENOUGH);
        }

        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(value));
        wallet.setBalance(wallet.getBalance().add(value));
        updateWallet(wallet);
        record(
            userId,
            WalletTransactionType.UNFREEZE,
            value,
            wallet.getBalance(),
            refType,
            refId,
            remark
        );
        return getWallet(userId);
    }

    /**
     * 从冻结金额中结算租金，不再回到可用余额。
     */
    @Transactional
    public WalletResponse consumeFrozen(
        Long userId,
        BigDecimal amount,
        String refType,
        Long refId,
        String remark
    ) {
        BigDecimal value = requirePositive(amount);
        Wallet wallet = getOrCreate(userId);
        if (wallet.getFrozenBalance().compareTo(value) < 0) {
            throw new BusinessException(ErrorCode.WALLET_FROZEN_NOT_ENOUGH);
        }

        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(value));
        updateWallet(wallet);
        record(
            userId,
            WalletTransactionType.RENT_PAY,
            value.negate(),
            wallet.getBalance(),
            refType,
            refId,
            remark
        );
        return getWallet(userId);
    }

    /**
     * 收入入账，例如出租收入或平台佣金返还。
     */
    @Transactional
    public WalletResponse income(
        Long userId,
        BigDecimal amount,
        String refType,
        Long refId,
        String remark
    ) {
        BigDecimal value = requirePositive(amount);
        Wallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance().add(value));
        wallet.setTotalIncome(wallet.getTotalIncome().add(value));
        updateWallet(wallet);
        record(
            userId,
            WalletTransactionType.INCOME,
            value,
            wallet.getBalance(),
            refType,
            refId,
            remark
        );
        return getWallet(userId);
    }

    /**
     * 退款入账。
     */
    @Transactional
    public WalletResponse refund(
        Long userId,
        BigDecimal amount,
        String refType,
        Long refId,
        String remark
    ) {
        BigDecimal value = requirePositive(amount);
        Wallet wallet = getOrCreate(userId);
        wallet.setBalance(wallet.getBalance().add(value));
        updateWallet(wallet);
        record(
            userId,
            WalletTransactionType.REFUND,
            value,
            wallet.getBalance(),
            refType,
            refId,
            remark
        );
        return getWallet(userId);
    }

    private Wallet getOrCreate(Long userId) {
        Wallet wallet = walletMapper.selectOne(
            Wrappers.<Wallet>lambdaQuery().eq(Wallet::getUserId, userId)
        );
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozenBalance(BigDecimal.ZERO);
            wallet.setTotalIncome(BigDecimal.ZERO);
            wallet.setVersion(0);
            walletMapper.insert(wallet);
        }
        return wallet;
    }

    private void updateWallet(Wallet wallet) {
        if (walletMapper.updateById(wallet) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
    }

    private void record(
        Long userId,
        WalletTransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String refType,
        Long refId,
        String remark
    ) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(type.name());
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRefType(refType);
        transaction.setRefId(refId);
        transaction.setRemark(remark);
        walletTransactionMapper.insert(transaction);
    }

    private BigDecimal recentRentSpent(Long userId) {
        List<WalletTransaction> transactions = walletTransactionMapper.selectList(
            Wrappers.<WalletTransaction>lambdaQuery()
                .eq(WalletTransaction::getUserId, userId)
                .eq(WalletTransaction::getType, WalletTransactionType.RENT_PAY.name())
                .ge(WalletTransaction::getCreatedAt, LocalDateTime.now().minusDays(30))
        );
        return transactions
            .stream()
            .map(t -> t.getAmount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "金额必须大于 0");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private WalletTransactionResponse toResponse(WalletTransaction transaction) {
        return new WalletTransactionResponse(
            transaction.getId(),
            transaction.getType(),
            transaction.getAmount(),
            transaction.getBalanceAfter(),
            transaction.getRefType(),
            transaction.getRefId(),
            transaction.getRemark(),
            transaction.getCreatedAt()
        );
    }
}
