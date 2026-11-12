package com.linzu.order.controller;

import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.order.dto.OrderResponse;
import com.linzu.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "订单（最小载体）")
@RestController
@RequestMapping({"/api/v1/orders", "/api/orders"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "我的订单（供发起纠纷选择）")
    @GetMapping("/mine")
    public Result<List<OrderResponse>> mine() {
        return Result.ok(orderService.listMine(AuthContext.requireUserId()));
    }
}
