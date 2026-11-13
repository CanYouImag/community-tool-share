package com.linzu.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_order_evaluation")
public class OrderEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long itemId;

    private Long lessorId;

    private Long renterId;

    private Long evaluatorId;

    private Long targetUserId;

    private Integer rating;

    private String content;

    private String imageUrls;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
