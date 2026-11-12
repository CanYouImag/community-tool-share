package com.linzu.dispute.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_dispute")
public class Dispute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long communityId;

    /** 发起纠纷的用户 ID */
    private Long initiatorId;

    /** 租客用户 ID（押金冻结在其钱包） */
    private Long renterId;

    /** 出租者用户 ID */
    private Long ownerId;

    private String reason;

    private BigDecimal depositAmount;

    private BigDecimal rentAmount;

    /** AWAITING_RENTER / MEDIATING / PENDING_ARBITRATION / RESOLVED / ARBITRATED / SETTLED */
    private String status;

    /** 物主提出扣留金额 */
    private BigDecimal withholdAmount;

    /** 租客回应说明（不同意时的异议理由） */
    private String renterNote;

    private String mediationConclusion;

    private String escalateNote;

    private LocalDateTime escalatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
