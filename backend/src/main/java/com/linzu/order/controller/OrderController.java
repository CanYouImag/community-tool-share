package com.linzu.order.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.order.dto.CreateOrderRequest;
import com.linzu.order.dto.OrderCancelRequest;
import com.linzu.order.dto.OrderInspectRequest;
import com.linzu.order.dto.OrderMessageResponse;
import com.linzu.order.dto.OrderPickupRequest;
import com.linzu.order.dto.OrderResponse;
import com.linzu.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单域接口：创建（D1）、支付/押金（D2）、接单（D3）、取物码（D4）、归还（D5）、
 * 验收（D6）、结算（D7）、取消（D8）、留言（D10）。
 */
@Tag(name = "订单")
@RestController
@RequestMapping({ "/api/v1/orders", "/api/orders" })
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（D1），返回待支付订单")
    @PostMapping
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "我的订单列表，role: renter(默认)/lessor，status 可选")
    @GetMapping
    public Result<PageResult<OrderResponse>> list(
        @RequestParam(defaultValue = "renter") String role,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(
            orderService.list(AuthContext.requireUserId(), role, status, page, size)
        );
    }

    @Operation(summary = "订单详情（含取物码视角控制，D4）")
    @GetMapping("/{id}")
    public Result<OrderResponse> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "支付订单（D2）：冻结租金与押金")
    @PostMapping("/{id}/pay")
    public Result<OrderResponse> pay(@PathVariable Long id) {
        return Result.ok(orderService.pay(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "接单（D3），生成取物码")
    @PostMapping("/{id}/accept")
    public Result<OrderResponse> accept(@PathVariable Long id) {
        return Result.ok(orderService.accept(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "拒单（D3），全额退款并取消")
    @PostMapping("/{id}/reject")
    public Result<OrderResponse> reject(
        @PathVariable Long id,
        @Valid @RequestBody RejectRequest request
    ) {
        return Result.ok(
            orderService.reject(AuthContext.requireUserId(), id, request.reason())
        );
    }

    @Operation(summary = "验码取物（D4）：出租人输入租客出示的取物码")
    @PostMapping("/{id}/pickup")
    public Result<OrderResponse> pickup(
        @PathVariable Long id,
        @Valid @RequestBody OrderPickupRequest request
    ) {
        return Result.ok(
            orderService.pickup(AuthContext.requireUserId(), id, request.code())
        );
    }

    @Operation(summary = "发起归还（D5）：使用中 → 待归还")
    @PostMapping("/{id}/return")
    public Result<OrderResponse> requestReturn(@PathVariable Long id) {
        return Result.ok(orderService.requestReturn(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "确认收到归还（D5）：待归还 → 待验收")
    @PostMapping("/{id}/confirm-return")
    public Result<OrderResponse> confirmReturn(@PathVariable Long id) {
        return Result.ok(orderService.confirmReturn(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "验收（D6）：通过则结算完成，不通过进入纠纷")
    @PostMapping("/{id}/inspect")
    public Result<OrderResponse> inspect(
        @PathVariable Long id,
        @Valid @RequestBody OrderInspectRequest request
    ) {
        return Result.ok(
            orderService.inspect(
                AuthContext.requireUserId(),
                id,
                Boolean.TRUE.equals(request.pass()),
                request.reason()
            )
        );
    }

    @Operation(summary = "取消订单（D8），已支付则全额退款")
    @PostMapping("/{id}/cancel")
    public Result<OrderResponse> cancel(
        @PathVariable Long id,
        @Valid @RequestBody OrderCancelRequest request
    ) {
        return Result.ok(
            orderService.cancel(AuthContext.requireUserId(), id, request.reason())
        );
    }

    @Operation(summary = "订单留言列表（D10）")
    @GetMapping("/{id}/messages")
    public Result<List<OrderMessageResponse>> messages(@PathVariable Long id) {
        return Result.ok(orderService.messages(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "发送订单留言（D10）")
    @PostMapping("/{id}/messages")
    public Result<OrderMessageResponse> sendMessage(
        @PathVariable Long id,
        @Valid @RequestBody MessageRequest request
    ) {
        return Result.ok(
            orderService.sendMessage(AuthContext.requireUserId(), id, request.content())
        );
    }

    public record RejectRequest(String reason) {}
    public record MessageRequest(String content) {}
}
