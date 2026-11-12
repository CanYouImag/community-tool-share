package com.linzu.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.order.dto.OrderResponse;
import com.linzu.order.entity.Order;
import com.linzu.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    /** 我的订单（租客或出租者视角），供发起纠纷选择 */
    public List<OrderResponse> listMine(Long userId) {
        List<Order> list = orderMapper.selectList(
                Wrappers.<Order>lambdaQuery()
                        .and(w -> w.eq(Order::getRenterId, userId)
                                .or().eq(Order::getOwnerId, userId))
                        .orderByDesc(Order::getId)
        );
        return list.stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId(), o.getItemId(), o.getItemTitle(), o.getCommunityId(),
                o.getRenterId(), o.getOwnerId(),
                o.getDepositAmount(), o.getRentAmount(),
                o.getStatus(), o.getCreatedAt());
    }
}
