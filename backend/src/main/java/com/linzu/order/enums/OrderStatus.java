package com.linzu.order.enums;

/**
 * 订单状态机：
 * PENDING_PAYMENT 待支付 → PENDING_ACCEPT 待接单 → PENDING_PICKUP 待取物 →
 * IN_USE 使用中 → PENDING_RETURN 待归还 → PENDING_INSPECT 待验收 →
 * COMPLETED 已完成 / CANCELLED 已取消 / DISPUTED 纠纷中
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PENDING_ACCEPT,
    PENDING_PICKUP,
    IN_USE,
    PENDING_RETURN,
    PENDING_INSPECT,
    COMPLETED,
    CANCELLED,
    DISPUTED
}
