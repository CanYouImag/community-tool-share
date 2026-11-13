package com.linzu.order.job;

import com.linzu.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理（UC-D9）：
 * - 待支付超时 → 自动取消
 * - 待接单 / 待取物超时 → 自动取消并退款
 * - 待归还 / 待验收超时 → 自动验收通过并结算
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void processTimeouts() {
        try {
            orderService.expirePayments();
            orderService.expireAcceptsAndPickups();
            orderService.markOverdueOrders();
            orderService.autoCompleteStaleOrders();
        } catch (Exception e) {
            log.error("订单超时任务执行失败", e);
        }
    }
}
