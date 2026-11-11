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

    /** MEDIATING / PENDING_ARBITRATION / RESOLVED / ARBITRATED */
    private String status;

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
