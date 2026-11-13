package com.linzu.item.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_item")
public class Item {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Long ownerId;

    private String title;

    private String description;

    /**
     * 成色：NEW 全新 / LIKE_NEW 九成新 / GOOD 八成新 / FAIR 明显使用痕迹
     */
    private String conditionLevel;

    /**
     * 日租金
     */
    private BigDecimal rentPerDay;

    /**
     * 押金
     */
    private BigDecimal deposit;

    /**
     * 所在社区/取还地点
     */
    private String location;

    /**
     * PENDING_REVIEW / REJECTED / LISTED / DELISTED / VIOLATION_OFF
     */
    private String status;

    /**
     * 平台置顶：1 是 0 否
     */
    private Integer isPinned;

    private String rejectReason;

    private String violationReason;

    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
