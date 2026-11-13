package com.linzu.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderEvaluationServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderEvaluationMapper evaluationMapper;
    @Mock
    private UserMapper userMapper;

    private OrderEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new OrderEvaluationService(
            orderMapper,
            evaluationMapper,
            userMapper
        );
    }

    @Test
    void renterCanEvaluateCompletedOrderWithinSevenDays() {
        Order order = completedOrder();
        User target = user(10L, 90);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(evaluationMapper.selectCount(any())).thenReturn(0L);
        when(evaluationMapper.insert(any(OrderEvaluation.class))).thenAnswer(invocation -> {
            invocation.<OrderEvaluation>getArgument(0).setId(9L);
            return 1;
        });
        when(userMapper.selectById(10L)).thenReturn(target);
        when(userMapper.updateById(target)).thenReturn(1);

        OrderEvaluationResponse response = evaluationService.create(
            20L,
            1L,
            new OrderEvaluationRequest(5, "很好", null)
        );

        assertEquals(5, response.rating());
        assertEquals(91, target.getCreditScore());
        verify(evaluationMapper).insert(any(OrderEvaluation.class));
    }

    @Test
    void duplicateEvaluationIsRejected() {
        when(orderMapper.selectById(1L)).thenReturn(completedOrder());
        when(evaluationMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> evaluationService.create(20L, 1L, new OrderEvaluationRequest(5, null, null))
        );

        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
        verify(evaluationMapper, never()).insert(any(OrderEvaluation.class));
    }

    @Test
    void evaluationWindowIsSevenDays() {
        Order order = completedOrder();
        order.setCompletedAt(LocalDateTime.now().minusDays(8));
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> evaluationService.create(20L, 1L, new OrderEvaluationRequest(5, null, null))
        );

        assertEquals(ErrorCode.PARAM_ERROR, exception.getErrorCode());
        verify(evaluationMapper, never()).insert(any(OrderEvaluation.class));
    }

    private Order completedOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setItemId(1L);
        order.setLessorId(10L);
        order.setRenterId(20L);
        order.setStatus(OrderStatus.COMPLETED.name());
        order.setCompletedAt(LocalDateTime.now().minusDays(1));
        return order;
    }

    private User user(Long id, int credit) {
        User user = new User();
        user.setId(id);
        user.setNickname("u" + id);
        user.setCreditScore(credit);
        return user;
    }
}
