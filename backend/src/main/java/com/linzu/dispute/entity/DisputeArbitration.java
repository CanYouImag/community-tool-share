package com.linzu.dispute.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_dispute_arbitration")
public class DisputeArbitration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long disputeId;

    private Long arbitratorId;

    /** REFUND_ALL / PARTIAL_DEDUCT / DEDUCT_ALL */
    private String verdictType;

    private BigDecimal deductAmount;

    private BigDecimal rentDiscount;

    private String awardText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
