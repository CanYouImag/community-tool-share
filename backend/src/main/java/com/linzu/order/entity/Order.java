package com.linzu.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务单号 */
    private String orderNo;

    private Long itemId;

    /** 下单时物品标题快照 */
    private String itemTitle;

    /** 下单时物品封面图快照 */
    private String itemImage;

    /** 所属社区 ID（下单时从物品快照） */
    private Long communityId;

    private Long lessorId;

    private Long renterId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer days;

    private BigDecimal rentAmount;

    private BigDecimal depositAmount;

    /** 平台佣金，结算时从租金中扣除 */
    private BigDecimal commissionAmount;

    private Integer overdueDays;

    private BigDecimal overdueFee;

    private Integer overduePenaltyApplied;

    /** PENDING_PAYMENT、PENDING_ACCEPT、PENDING_PICKUP、IN_USE、PENDING_RETURN、PENDING_INSPECT、COMPLETED、CANCELLED、DISPUTED */
    private String status;

    /** 取物码，验码取物后置空 */
    private String pickupCode;

    private String cancelReason;

    private LocalDateTime paidAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime pickupAt;

    private LocalDateTime returnAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
