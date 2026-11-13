package com.linzu.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.item.entity.Item;
import com.linzu.item.enums.ItemStatus;
import com.linzu.item.mapper.ItemImageMapper;
import com.linzu.item.mapper.ItemMapper;
import com.linzu.order.dto.CreateOrderRequest;
import com.linzu.order.dto.OrderResponse;
import com.linzu.order.entity.Order;
import com.linzu.order.enums.OrderStatus;
import com.linzu.order.mapper.OrderMapper;
import com.linzu.order.mapper.OrderMessageMapper;
import com.linzu.platform.entity.PlatformConfig;
import com.linzu.platform.mapper.PlatformConfigMapper;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import com.linzu.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderMessageMapper orderMessageMapper;
    @Mock
    private ItemMapper itemMapper;
    @Mock
    private ItemImageMapper itemImageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private WalletService walletService;
    @Mock
    private com.linzu.community.mapper.CommunityMemberMapper communityMemberMapper;
    @Mock
    private PlatformConfigMapper platformConfigMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderMapper,
            orderMessageMapper,
            itemMapper,
            itemImageMapper,
            userMapper,
            walletService,
            communityMemberMapper,
            platformConfigMapper
        );
        ReflectionTestUtils.setField(orderService, "commissionRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(orderService, "timeoutReturnDays", 7L);
    }

    @Test
    void createCountsBothStartAndEndDate() {
        Item item = listedItem();
        User renter = user(20L, "REGISTERED", 80);
        User owner = user(10L, "CERTIFIED", 100);
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(userMapper.selectById(20L)).thenReturn(renter);
        when(userMapper.selectById(10L)).thenReturn(owner);
        when(communityMemberMapper.selectCount(any())).thenReturn(1L);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(itemImageMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            invocation.<Order>getArgument(0).setId(99L);
            return 1;
        });

        OrderResponse response = orderService.create(
            20L,
            new CreateOrderRequest(1L, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12))
        );

        assertEquals(3, response.days());
        assertEquals(new BigDecimal("45.00"), response.rentAmount());
    }

    @Test
    void createRejectsOverlappingOrder() {
        when(itemMapper.selectById(1L)).thenReturn(listedItem());
        when(userMapper.selectById(20L)).thenReturn(user(20L, "REGISTERED", 80));
        when(communityMemberMapper.selectCount(any())).thenReturn(1L);
        when(orderMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () ->
                orderService.create(
                    20L,
                    new CreateOrderRequest(
                        1L,
                        LocalDate.of(2026, 12, 10),
                        LocalDate.of(2026, 12, 12)
                    )
                )
        );

        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void inspectSettlesWithPlatformCommissionConfig() {
        Order order = order(5L, OrderStatus.PENDING_INSPECT);
        when(orderMapper.selectById(5L)).thenReturn(order);
        when(platformConfigMapper.selectOne(any())).thenReturn(config("0.15"));
        when(orderMapper.update(any(Order.class), any())).thenReturn(1);
        when(userMapper.selectById(10L)).thenReturn(user(10L, "CERTIFIED", 100));
        when(userMapper.selectById(20L)).thenReturn(user(20L, "REGISTERED", 80));

        orderService.inspect(10L, 5L, true, null);

        assertEquals(new BigDecimal("4.50"), order.getCommissionAmount());
        assertEquals(OrderStatus.COMPLETED.name(), order.getStatus());
        assertNotNull(order.getCompletedAt());
        verify(walletService).consumeFrozen(
            eq(20L),
            eq(new BigDecimal("30.00")),
            eq("ORDER"),
            eq(5L),
            any()
        );
        verify(walletService).income(
            eq(10L),
            eq(new BigDecimal("25.50")),
            eq("ORDER"),
            eq(5L),
            any()
        );
    }

    @Test
    void autoCompleteUsesPreviousStatusAsGuard() {
        Order order = order(6L, OrderStatus.PENDING_RETURN);
        order.setUpdatedAt(LocalDateTime.now().minusDays(10));
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        when(platformConfigMapper.selectOne(any())).thenReturn(config("0.10"));
        when(orderMapper.update(any(Order.class), any())).thenReturn(1);

        orderService.autoCompleteStaleOrders();

        assertEquals(OrderStatus.COMPLETED.name(), order.getStatus());
        verify(orderMapper).update(eq(order), any());
    }

    @Test
    void cancelAfterAcceptIsRejected() {
        Order order = order(7L, OrderStatus.PENDING_PICKUP);
        when(orderMapper.selectById(7L)).thenReturn(order);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> orderService.cancel(20L, 7L, "不想要了")
        );

        assertEquals(ErrorCode.ORDER_STATE_NOT_ALLOWED, exception.getErrorCode());
        verify(walletService, never()).unfreeze(anyLong(), any(), any(), anyLong(), any());
    }

    private Item listedItem() {
        Item item = new Item();
        item.setId(1L);
        item.setOwnerId(10L);
        item.setCommunityId(1L);
        item.setTitle("电钻");
        item.setRentPerDay(new BigDecimal("15.00"));
        item.setDeposit(new BigDecimal("300.00"));
        item.setStatus(ItemStatus.LISTED.name());
        return item;
    }

    private Order order(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setLessorId(10L);
        order.setRenterId(20L);
        order.setRentAmount(new BigDecimal("30.00"));
        order.setDepositAmount(new BigDecimal("300.00"));
        order.setStatus(status.name());
        order.setCreatedAt(LocalDateTime.now().minusDays(10));
        order.setUpdatedAt(LocalDateTime.now().minusDays(10));
        return order;
    }

    private User user(Long id, String role, int credit) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setCreditScore(credit);
        user.setNickname("user" + id);
        return user;
    }

    private PlatformConfig config(String value) {
        PlatformConfig config = new PlatformConfig();
        config.setConfigKey("commission_rate");
        config.setConfigValue(value);
        return config;
    }
}
