package com.linzu.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.order.dto.OrderEvaluationRequest;
import com.linzu.order.dto.OrderEvaluationResponse;
import com.linzu.order.entity.Order;
import com.linzu.order.entity.OrderEvaluation;
import com.linzu.order.enums.OrderStatus;
import com.linzu.order.mapper.OrderEvaluationMapper;
import com.linzu.order.mapper.OrderMapper;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderEvaluationService {

    private static final int EVALUATION_WINDOW_DAYS = 7;

    private final OrderMapper orderMapper;
    private final OrderEvaluationMapper evaluationMapper;
    private final UserMapper userMapper;

    @Transactional
    public OrderEvaluationResponse create(Long userId, Long orderId, OrderEvaluationRequest request) {
        Order order = requireOrder(orderId);
        if (!userId.equals(order.getRenterId()) && !userId.equals(order.getLessorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单完成后才能评价");
        }
        if (
            order.getCompletedAt() != null &&
            order.getCompletedAt().plusDays(EVALUATION_WINDOW_DAYS).isBefore(LocalDateTime.now())
        ) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评价窗口已关闭（完成后 7 天内可评价）");
        }
        Long exists = evaluationMapper.selectCount(
            Wrappers
                .<OrderEvaluation>lambdaQuery()
                .eq(OrderEvaluation::getOrderId, orderId)
                .eq(OrderEvaluation::getEvaluatorId, userId)
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "该订单已经评价过，不能重复评价");
        }

        Long targetUserId = userId.equals(order.getRenterId())
            ? order.getLessorId()
            : order.getRenterId();
        OrderEvaluation evaluation = new OrderEvaluation();
        evaluation.setOrderId(orderId);
        evaluation.setItemId(order.getItemId());
        evaluation.setLessorId(order.getLessorId());
        evaluation.setRenterId(order.getRenterId());
        evaluation.setEvaluatorId(userId);
        evaluation.setTargetUserId(targetUserId);
        evaluation.setRating(request.rating());
        evaluation.setContent(
            StringUtils.hasText(request.content()) ? request.content().trim() : null
        );
        evaluation.setImageUrls(
            request.imageUrls() == null || request.imageUrls().isEmpty()
                ? null
                : request.imageUrls()
                    .stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.joining(","))
        );
        evaluationMapper.insert(evaluation);

        if (request.rating() != null && request.rating() >= 4) {
            User target = userMapper.selectById(targetUserId);
            if (target != null) {
                int score = target.getCreditScore() == null ? 80 : target.getCreditScore();
                target.setCreditScore(Math.min(100, score + 1));
                userMapper.updateById(target);
            }
        }
        return toResponse(evaluation);
    }

    public List<OrderEvaluationResponse> listByOrder(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (!userId.equals(order.getRenterId()) && !userId.equals(order.getLessorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return evaluationMapper
            .selectList(
                Wrappers
                    .<OrderEvaluation>lambdaQuery()
                    .eq(OrderEvaluation::getOrderId, orderId)
                    .orderByAsc(OrderEvaluation::getId)
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<OrderEvaluationResponse> listByItem(Long itemId) {
        return evaluationMapper
            .selectList(
                Wrappers
                    .<OrderEvaluation>lambdaQuery()
                    .eq(OrderEvaluation::getItemId, itemId)
                    .orderByDesc(OrderEvaluation::getId)
                    .last("LIMIT 50")
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderEvaluationResponse toResponse(OrderEvaluation evaluation) {
        User evaluator = userMapper.selectById(evaluation.getEvaluatorId());
        return new OrderEvaluationResponse(
            evaluation.getId(),
            evaluation.getOrderId(),
            evaluation.getItemId(),
            evaluation.getEvaluatorId(),
            evaluator == null ? null : evaluator.getNickname(),
            evaluation.getTargetUserId(),
            evaluation.getRating(),
            evaluation.getContent(),
            evaluation.getImageUrls(),
            evaluation.getCreatedAt()
        );
    }
}
