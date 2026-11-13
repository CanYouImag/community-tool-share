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
@TableName("t_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;

    /** 关联社区 ID（BR-13 行级隔离） */
    private Long communityId;

    /** ITEM / USER / REVIEW */
    private String targetType;

    private Long targetId;

    /** FALSE_INFO / ILLEGAL_CONTENT / MALICIOUS_REVIEW / OTHER */
    private String reasonType;

    private String description;

    private String evidenceUrls;

    /** PENDING / PROCESSED */
    private String status;

    /** REJECT / ITEM_DOWN / WARN / BAN */
    private String handleAction;

    private String handleNote;

    private Long handledBy;

    private LocalDateTime handledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
