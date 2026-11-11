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
@TableName("t_dispute_evidence")
public class DisputeEvidence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long disputeId;

    private String imageUrl;

    /** UPLOAD 上传凭证 / PICKUP 取物存证 / RETURN 归还存证 */
    private String evidenceType;

    private Long uploadedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
