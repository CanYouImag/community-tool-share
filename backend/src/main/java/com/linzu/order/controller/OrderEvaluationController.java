package com.linzu.order.controller;

import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.order.dto.OrderEvaluationRequest;
import com.linzu.order.dto.OrderEvaluationResponse;
import com.linzu.order.service.OrderEvaluationService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单评价")
@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
public class OrderEvaluationController {

    private final OrderEvaluationService evaluationService;

    @Operation(summary = "提交订单评价（完成后 7 天内，每方一次）")
    @PostMapping("/orders/{orderId}/evaluations")
    public Result<OrderEvaluationResponse> create(
        @PathVariable Long orderId,
        @Valid @RequestBody OrderEvaluationRequest request
    ) {
        return Result.ok(
            evaluationService.create(AuthContext.requireUserId(), orderId, request)
        );
    }

    @Operation(summary = "订单评价列表（当事人可见）")
    @GetMapping("/orders/{orderId}/evaluations")
    public Result<List<OrderEvaluationResponse>> listByOrder(@PathVariable Long orderId) {
        return Result.ok(
            evaluationService.listByOrder(AuthContext.requireUserId(), orderId)
        );
    }

    @Operation(summary = "物品评价列表（公开）")
    @GetMapping("/items/{itemId}/evaluations")
    public Result<List<OrderEvaluationResponse>> listByItem(@PathVariable Long itemId) {
        return Result.ok(evaluationService.listByItem(itemId));
    }
}
