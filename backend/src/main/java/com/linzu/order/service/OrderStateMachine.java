package com.linzu.order.service;

import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.order.enums.OrderStatus;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机（UC-D）：统一校验状态流转合法性。
 */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.PENDING_ACCEPT, OrderStatus.CANCELLED),
        OrderStatus.PENDING_ACCEPT, Set.of(OrderStatus.PENDING_PICKUP, OrderStatus.CANCELLED),
        OrderStatus.PENDING_PICKUP, Set.of(OrderStatus.IN_USE, OrderStatus.CANCELLED),
        OrderStatus.IN_USE, Set.of(OrderStatus.PENDING_RETURN),
        OrderStatus.PENDING_RETURN, Set.of(OrderStatus.PENDING_INSPECT, OrderStatus.CANCELLED),
        OrderStatus.PENDING_INSPECT, Set.of(OrderStatus.COMPLETED, OrderStatus.DISPUTED, OrderStatus.CANCELLED),
        OrderStatus.DISPUTED, Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED),
        OrderStatus.COMPLETED, Set.of(),
        OrderStatus.CANCELLED, Set.of()
    );

    private OrderStateMachine() {}

    public static void check(OrderStatus current, OrderStatus target) {
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException(
                ErrorCode.ORDER_STATE_NOT_ALLOWED,
                "订单不允许从 " + current + " 变更为 " + target
            );
        }
    }

    public static boolean canCancel(OrderStatus current) {
        return TRANSITIONS.getOrDefault(current, Set.of()).contains(OrderStatus.CANCELLED);
    }
}
