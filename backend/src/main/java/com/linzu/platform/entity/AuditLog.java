package com.linzu.platform.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String action;

    private String targetType;

    private Long targetId;

    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
