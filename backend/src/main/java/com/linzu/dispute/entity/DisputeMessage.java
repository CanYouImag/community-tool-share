package com.linzu.dispute.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_dispute_message")
public class DisputeMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long disputeId;

    private Long senderId;

    /** USER / COMMUNITY_ADMIN / PLATFORM_ADMIN */
    private String senderRole;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
