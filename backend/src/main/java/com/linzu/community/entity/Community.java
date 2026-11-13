package com.linzu.community.entity;

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
@TableName("t_community")
public class Community {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String intro;

    private String coverUrl;

    /**
     * 加入申请自动通过开关：true 自动通过，false 需社区管理员审核（BR-16）
     */
    private Boolean autoApprove;

    private Integer memberCount;

    private Integer itemCount;

    /**
     * ACTIVE / DISABLED
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
