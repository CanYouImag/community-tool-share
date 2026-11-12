package com.linzu.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linzu.common.exception.BusinessException;
import com.linzu.user.entity.User;
import com.linzu.user.service.UserService;
import com.linzu.wallet.dto.WalletResponse;
import com.linzu.wallet.entity.Wallet;
import com.linzu.wallet.entity.WalletTransaction;
import com.linzu.wallet.mapper.WalletMapper;
import com.linzu.wallet.mapper.WalletTransactionMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private WalletService walletService;

    @Test
    void adjustCreditsBalanceAndRecordsTransaction() {
        User user = activeUser(7L);
        Wallet wallet = wallet(7L, "10.00");
        when(userService.requireUser(7L)).thenReturn(user);
        when(userService.isBanned(user)).thenReturn(false);
        when(walletMapper.selectOne(any())).thenReturn(wallet);
        when(walletMapper.updateById(any(Wallet.class))).thenReturn(1);
        when(walletTransactionMapper.insert(any(WalletTransaction.class))).thenReturn(1);
        when(walletTransactionMapper.selectList(any())).thenReturn(List.of());

        WalletResponse response = walletService.adjust(
            7L,
            new BigDecimal("5.00"),
            "测试充值"
        );

        assertEquals(new BigDecimal("15.00"), response.balance());
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(
            WalletTransaction.class
        );
        verify(walletTransactionMapper).insert(captor.capture());
        assertEquals("ADMIN_ADJUST", captor.getValue().getType());
        assertEquals(new BigDecimal("5.00"), captor.getValue().getAmount());
        assertEquals(new BigDecimal("15.00"), captor.getValue().getBalanceAfter());
        assertEquals("测试充值", captor.getValue().getRemark());
    }

    @Test
    void adjustRejectsNegativeResultingBalance() {
        User user = activeUser(7L);
        Wallet wallet = wallet(7L, "10.00");
        when(userService.requireUser(7L)).thenReturn(user);
        when(userService.isBanned(user)).thenReturn(false);
        when(walletMapper.selectOne(any())).thenReturn(wallet);

        assertThrows(
            BusinessException.class,
            () -> walletService.adjust(7L, new BigDecimal("-20.00"), null)
        );
        verify(walletMapper, never()).updateById(any(Wallet.class));
    }

    @Test
    void adjustRejectsZeroAmount() {
        assertThrows(
            BusinessException.class,
            () -> walletService.adjust(7L, BigDecimal.ZERO, null)
        );
    }

    @Test
    void adjustRejectsBannedUser() {
        User user = activeUser(7L);
        when(userService.requireUser(7L)).thenReturn(user);
        when(userService.isBanned(user)).thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> walletService.adjust(7L, new BigDecimal("5.00"), null)
        );
        verify(walletMapper, never()).updateById(any(Wallet.class));
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus("ACTIVE");
        return user;
    }

    private Wallet wallet(Long userId, String balance) {
        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal(balance));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        wallet.setVersion(0);
        return wallet;
    }
}
