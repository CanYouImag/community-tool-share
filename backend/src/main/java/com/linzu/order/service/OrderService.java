package com.linzu.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.item.entity.Item;
import com.linzu.item.entity.ItemImage;
import com.linzu.item.enums.ItemStatus;
import com.linzu.item.mapper.ItemImageMapper;
import com.linzu.item.mapper.ItemMapper;
import com.linzu.order.dto.CreateOrderRequest;
import com.linzu.order.dto.OrderMessageResponse;
import com.linzu.order.dto.OrderResponse;
import com.linzu.order.entity.Order;
import com.linzu.order.entity.OrderMessage;
import com.linzu.order.enums.OrderStatus;
import com.linzu.order.mapper.OrderMapper;
import com.linzu.order.mapper.OrderMessageMapper;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import com.linzu.wallet.service.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单域：创建（D1）、支付/押金（D2）、接单（D3）、取物码（D4）、归还（D5）、
 * 验收（D6）、结算（D7）、取消（D8）、超时（D9）、留言（D10）。
 *
 * 资金流转（复用 wallet 域）：
 * 支付冻结租客租金与押金（freeze）；取消或拒单全额解冻退回（unfreeze）；
 * 结算时租金从冻结中划扣（consumeFrozen）后出租人入账（income，扣佣金），
 * 押金解冻退回租客（unfreeze）。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String REF_TYPE_ORDER = "ORDER";
    private static final DateTimeFormatter ORDER_NO_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderMessageMapper orderMessageMapper;
    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final UserMapper userMapper;
    private final WalletService walletService;

    @Value("${app.order.commission-rate:0}")
    private BigDecimal commissionRate;

    @Value("${app.order.timeout-pay-minutes:15}")
    private long timeoutPayMinutes;

    @Value("${app.order.timeout-accept-hours:24}")
    private long timeoutAcceptHours;

    @Value("${app.order.timeout-pickup-hours:48}")
    private long timeoutPickupHours;

    @Value("${app.order.timeout-return-days:7}")
    private long timeoutReturnDays;

    @Transactional
    public OrderResponse create(Long renterId, CreateOrderRequest request) {
        Item item = itemMapper.selectById(request.itemId());
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!ItemStatus.LISTED.name().equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.ITEM_NOT_AVAILABLE);
        }
        if (renterId.equals(item.getOwnerId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能租用自己的物品");
        }
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        if (end.isBefore(start) || start.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.RENT_PERIOD_INVALID);
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end));
        BigDecimal rent = item.getRentPerDay()
            .multiply(BigDecimal.valueOf(days))
            .setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setItemId(item.getId());
        order.setItemTitle(item.getTitle());
        order.setItemImage(firstImage(item.getId()));
        order.setLessorId(item.getOwnerId());
        order.setRenterId(renterId);
        order.setStartDate(start);
        order.setEndDate(end);
        order.setDays((int) days);
        order.setRentAmount(rent);
        order.setDepositAmount(item.getDeposit() == null
            ? BigDecimal.ZERO
            : item.getDeposit().setScale(2, RoundingMode.HALF_UP));
        order.setCommissionAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        orderMapper.insert(order);
        return toResponse(order, null);
    }

    @Transactional
    public OrderResponse pay(Long renterId, Long orderId) {
        Order order = requireOrder(orderId);
        requireParticipant(order, renterId);
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在待支付状态");
        }
        BigDecimal total = order.getRentAmount().add(order.getDepositAmount());
        walletService.freeze(renterId, total, REF_TYPE_ORDER, orderId, "订单支付：租金与押金冻结");
        order.setStatus(OrderStatus.PENDING_ACCEPT.name());
        order.setPaidAt(LocalDateTime.now());
        updateWithStatusGuard(order, OrderStatus.PENDING_PAYMENT);
        return toResponse(order, renterId);
    }

    @Transactional
    public OrderResponse accept(Long lessorId, Long orderId) {
        Order order = requireOrder(orderId);
        requireParticipant(order, lessorId);
        if (!OrderStatus.PENDING_ACCEPT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在待接单状态");
        }
        order.setStatus(OrderStatus.PENDING_PICKUP.name());
        order.setAcceptedAt(LocalDateTime.now());
        order.setPickupCode(generatePickupCode());
        updateWithStatusGuard(order, OrderStatus.PENDING_ACCEPT);
        return toResponse(order, lessorId);
    }

    @Transactional
    public OrderResponse reject(Long lessorId, Long orderId, String reason) {
        Order order = requireOrder(orderId);
        requireParticipant(order, lessorId);
        if (!OrderStatus.PENDING_ACCEPT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在待接单状态");
        }
        cancelAndRefund(order, OrderStatus.PENDING_ACCEPT, "出租人拒单：" + nullSafe(reason));
        return toResponse(order, lessorId);
    }

    @Transactional
    public OrderResponse pickup(Long lessorId, Long orderId, String code) {
        Order order = requireOrder(orderId);
        requireParticipant(order, lessorId);
        if (!OrderStatus.PENDING_PICKUP.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在待取物状态");
        }
        if (order.getPickupCode() == null
            || !order.getPickupCode().equals(code == null ? "" : code.trim())) {
            throw new BusinessException(ErrorCode.PICKUP_CODE_ERROR);
        }
        order.setStatus(OrderStatus.IN_USE.name());
        order.setPickupAt(LocalDateTime.now());
        order.setPickupCode(null);
        updateWithStatusGuard(order, OrderStatus.PENDING_PICKUP);
        return toResponse(order, lessorId);
    }

    @Transactional
    public OrderResponse requestReturn(Long renterId, Long orderId) {
        Order order = requireOrder(orderId);
        requireParticipant(order, renterId);
        if (!OrderStatus.IN_USE.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在使用中状态");
        }
        order.setStatus(OrderStatus.PENDING_RETURN.name());
        order.setReturnAt(LocalDateTime.now());
        updateWithStatusGuard(order, OrderStatus.IN_USE);
        return toResponse(order, renterId);
    }

    @Transactional
    public OrderResponse confirmReturn(Long lessorId, Long orderId) {
        Order order = requireOrder(orderId);
        requireParticipant(order, lessorId);
        if (!OrderStatus.PENDING_RETURN.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在待归还状态");
        }
        order.setStatus(OrderStatus.PENDING_INSPECT.name());
        updateWithStatusGuard(order, OrderStatus.PENDING_RETURN);
        return toResponse(order, lessorId);
    }

    @Transactional
    public OrderResponse inspect(Long lessorId, Long orderId, boolean pass, String reason) {
        Order order = requireOrder(orderId);
        requireParticipant(order, lessorId);
        if (!OrderStatus.PENDING_INSPECT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_NOT_ALLOWED, "订单不在待验收状态");
        }
        if (pass) {
            settle(order);
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "验收不通过必须填写原因");
            }
            order.setStatus(OrderStatus.DISPUTED.name());
            order.setCancelReason("验收不通过：" + reason.trim());
        }
        updateWithStatusGuard(order, OrderStatus.PENDING_INSPECT);
        return toResponse(order, lessorId);
    }

    private void settle(Order order) {
        BigDecimal rent = order.getRentAmount();
        BigDecimal deposit = order.getDepositAmount();

        walletService.consumeFrozen(
            order.getRenterId(), rent, REF_TYPE_ORDER, order.getId(), "订单结算：租金划扣"
        );
        BigDecimal commission = rent.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal income = rent.subtract(commission);
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            walletService.income(
                order.getLessorId(), income, REF_TYPE_ORDER, order.getId(),
                "订单结算：租金入账（佣金 " + commission + "）"
            );
        }
        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            walletService.unfreeze(
                order.getRenterId(), deposit, REF_TYPE_ORDER, order.getId(), "订单结算：押金退还"
            );
        }

        order.setCommissionAmount(commission);
        order.setStatus(OrderStatus.COMPLETED.name());
        order.setCompletedAt(LocalDateTime.now());
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long orderId, String reason) {
        Order order = requireOrder(orderId);
        requireParticipant(order, userId);
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        OrderStateMachine.check(current, OrderStatus.CANCELLED);
        cancelAndRefund(order, current, nullSafe(reason));
        return toResponse(order, userId);
    }

    private void cancelAndRefund(Order order, OrderStatus expected, String reason) {
        BigDecimal total = order.getRentAmount().add(order.getDepositAmount());
        if (order.getPaidAt() != null) {
            walletService.unfreeze(
                order.getRenterId(), total, REF_TYPE_ORDER, order.getId(), "订单取消：全额退回"
            );
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        updateWithStatusGuard(order, expected);
    }

    @Transactional
    public void expirePayments() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutPayMinutes);
        List<Order> orders = orderMapper.selectList(
            Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.name())
                .lt(Order::getCreatedAt, deadline)
        );
        for (Order order : orders) {
            order.setStatus(OrderStatus.CANCELLED.name());
            order.setCancelReason("超时未支付，系统自动取消");
            order.setCancelledAt(LocalDateTime.now());
            updateWithStatusGuard(order, OrderStatus.PENDING_PAYMENT);
        }
    }

    @Transactional
    public void expireAcceptsAndPickups() {
        cancelExpired(
            OrderStatus.PENDING_ACCEPT,
            LocalDateTime.now().minusHours(timeoutAcceptHours),
            "超时未接单，系统自动取消并退款"
        );
        cancelExpired(
            OrderStatus.PENDING_PICKUP,
            LocalDateTime.now().minusHours(timeoutPickupHours),
            "超时未取物，系统自动取消并退款"
        );
    }

    @Transactional
    public void autoCompleteStaleOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(timeoutReturnDays);
        List<Order> orders = orderMapper.selectList(
            Wrappers.<Order>lambdaQuery()
                .in(Order::getStatus,
                    OrderStatus.PENDING_RETURN.name(), OrderStatus.PENDING_INSPECT.name())
                .lt(Order::getUpdatedAt, deadline)
        );
        for (Order order : orders) {
            settle(order);
            order.setCancelReason("出租人超时未验收，系统自动完成结算");
            updateWithStatusGuard(order, OrderStatus.valueOf(order.getStatus()));
        }
    }

    private void cancelExpired(OrderStatus status, LocalDateTime deadline, String reason) {
        List<Order> orders = orderMapper.selectList(
            Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, status.name())
                .lt(Order::getUpdatedAt, deadline)
        );
        for (Order order : orders) {
            cancelAndRefund(order, status, reason);
        }
    }

    @Transactional
    public OrderResponse resolveDispute(
        Long orderId,
        String outcome,
        BigDecimal deductAmount,
        String reason
    ) {
        Order order = requireOrder(orderId);
        if (!OrderStatus.DISPUTED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATE_NOT_ALLOWED);
        }
        OrderStatus target = "CANCELLED".equals(outcome)
            ? OrderStatus.CANCELLED
            : "COMPLETED".equals(outcome) ? OrderStatus.COMPLETED : null;
        if (target == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "调解结论仅支持 COMPLETED / CANCELLED");
        }

        if (target == OrderStatus.CANCELLED) {
            cancelAndRefund(order, OrderStatus.DISPUTED, "平台调解：" + nullSafe(reason));
        } else {
            settle(order);
            BigDecimal deposit = order.getDepositAmount();
            BigDecimal deduct = deductAmount == null
                ? BigDecimal.ZERO
                : deductAmount.setScale(2, RoundingMode.HALF_UP);
            if (deduct.compareTo(BigDecimal.ZERO) > 0) {
                if (deduct.compareTo(deposit) > 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "扣款金额不能超过押金");
                }
                walletService.consumeFrozen(
                    order.getRenterId(), deduct, REF_TYPE_ORDER, order.getId(), "纠纷调解：押金扣除"
                );
                walletService.income(
                    order.getLessorId(), deduct, REF_TYPE_ORDER, order.getId(), "纠纷调解：押金赔偿入账"
                );
                BigDecimal remain = deposit.subtract(deduct);
                if (remain.compareTo(BigDecimal.ZERO) > 0) {
                    walletService.unfreeze(
                        order.getRenterId(), remain, REF_TYPE_ORDER, order.getId(),
                        "纠纷调解：剩余押金退还"
                    );
                }
            }
            order.setCompletedAt(LocalDateTime.now());
        }
        updateWithStatusGuard(order, OrderStatus.DISPUTED);
        return toResponse(order, null);
    }

    public PageResult<OrderResponse> list(Long userId, String role, String status, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        boolean asLessor = "lessor".equalsIgnoreCase(role);

        Page<Order> result = orderMapper.selectPage(
            new Page<>(safePage, safeSize),
            Wrappers.<Order>lambdaQuery()
                .eq(asLessor, Order::getLessorId, userId)
                .eq(!asLessor, Order::getRenterId, userId)
                .eq(StringUtils.hasText(status), Order::getStatus, status)
                .orderByDesc(Order::getId)
        );
        List<OrderResponse> records = result.getRecords().stream()
            .map(order -> toResponse(order, userId))
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public OrderResponse detail(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        requireParticipant(order, userId);
        return toResponse(order, userId);
    }

    public List<Map<String, Object>> recentDeposits(Long userId) {
        return orderMapper.selectList(
                Wrappers.<Order>lambdaQuery()
                    .eq(Order::getRenterId, userId)
                    .gt(Order::getDepositAmount, BigDecimal.ZERO)
                    .orderByDesc(Order::getId)
                    .last("LIMIT 20")
            )
            .stream()
            .map(order -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("orderId", order.getId());
                row.put("orderNo", order.getOrderNo());
                row.put("itemTitle", order.getItemTitle());
                row.put("itemImage", order.getItemImage());
                row.put("amount", order.getDepositAmount());
                row.put("status", order.getStatus());
                row.put("createdAt", order.getCreatedAt());
                return row;
            })
            .toList();
    }

    public List<OrderMessageResponse> messages(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        requireParticipant(order, userId);
        List<OrderMessage> messages = orderMessageMapper.selectList(
            Wrappers.<OrderMessage>lambdaQuery()
                .eq(OrderMessage::getOrderId, orderId)
                .orderByAsc(OrderMessage::getId)
        );
        Map<Long, User> senders = loadSenders(messages);
        return messages.stream()
            .map(message -> {
                User sender = senders.get(message.getSenderId());
                return new OrderMessageResponse(
                    message.getId(),
                    message.getOrderId(),
                    message.getSenderId(),
                    sender == null ? null : sender.getNickname(),
                    sender == null ? null : sender.getAvatarUrl(),
                    message.getContent(),
                    message.getCreatedAt()
                );
            })
            .toList();
    }

    @Transactional
    public OrderMessageResponse sendMessage(Long userId, Long orderId, String content) {
        Order order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (!StringUtils.hasText(content) || content.isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_MESSAGE_EMPTY);
        }
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setSenderId(userId);
        message.setContent(content.trim());
        orderMessageMapper.insert(message);
        User sender = userMapper.selectById(userId);
        return new OrderMessageResponse(
            message.getId(),
            message.getOrderId(),
            userId,
            sender == null ? null : sender.getNickname(),
            sender == null ? null : sender.getAvatarUrl(),
            message.getContent(),
            message.getCreatedAt()
        );
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void requireParticipant(Order order, Long userId) {
        if (!userId.equals(order.getRenterId()) && !userId.equals(order.getLessorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void updateWithStatusGuard(Order order, OrderStatus expected) {
        boolean ok = orderMapper.update(
            order,
            Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, expected.name())
        ) > 0;
        if (!ok) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
    }

    private String generateOrderNo() {
        return "LZ" + LocalDateTime.now().format(ORDER_NO_FORMAT)
            + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String generatePickupCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    private String firstImage(Long itemId) {
        ItemImage image = itemImageMapper.selectOne(
            Wrappers.<ItemImage>lambdaQuery()
                .eq(ItemImage::getItemId, itemId)
                .orderByAsc(ItemImage::getSort)
                .orderByAsc(ItemImage::getId)
                .last("LIMIT 1")
        );
        return image == null ? null : image.getUrl();
    }

    private Map<Long, User> loadSenders(List<OrderMessage> messages) {
        List<Long> ids = messages.stream().map(OrderMessage::getSenderId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private OrderResponse toResponse(Order order, Long viewerId) {
        boolean pickupVisible =
            OrderStatus.PENDING_PICKUP.name().equals(order.getStatus())
                && order.getPickupCode() != null
                && (viewerId == null
                    || viewerId.equals(order.getRenterId())
                    || viewerId.equals(order.getLessorId()));
        return new OrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getItemId(),
            order.getItemTitle(),
            order.getItemImage(),
            order.getLessorId(),
            order.getRenterId(),
            order.getStartDate(),
            order.getEndDate(),
            order.getDays(),
            order.getRentAmount(),
            order.getDepositAmount(),
            order.getCommissionAmount(),
            order.getStatus(),
            pickupVisible ? order.getPickupCode() : null,
            order.getCancelReason(),
            order.getPaidAt(),
            order.getAcceptedAt(),
            order.getPickupAt(),
            order.getReturnAt(),
            order.getCompletedAt(),
            order.getCancelledAt(),
            order.getCreatedAt()
        );
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
