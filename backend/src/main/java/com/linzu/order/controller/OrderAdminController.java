package com.linzu.order.controller;

import com.linzu.common.api.Result;
import com.linzu.common.security.RequirePermission;
import com.linzu.order.dto.OrderResponse;
import com.linzu.order.dto.OrderResolveRequest;
import com.linzu.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单平台管理接口：纠纷调解仲裁。
 */
@Tag(name = "订单平台管理")
@RestController
@RequestMapping({ "/api/v1/admin/orders", "/api/admin/orders" })
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @Operation(summary = "平台调解纠纷：outcome=COMPLETED 正常结算（可扣押金）/ CANCELLED 全额退款")
    @RequirePermission("platform:order-resolve")
    @PostMapping("/{id}/resolve")
    public Result<OrderResponse> resolve(
        @PathVariable Long id,
        @Valid @RequestBody OrderResolveRequest request
    ) {
        return Result.ok(
            orderService.resolveDispute(
                id,
                request.outcome(),
                request.deductAmount(),
                request.reason()
            )
        );
    }

}
