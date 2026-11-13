package com.linzu.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 物品发布 / 编辑请求（UC-C5）
 */
public record ItemSaveRequest(
    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题最长 64 字")
    String title,

    @NotNull(message = "分类不能为空")
    Long categoryId,

    @Size(max = 2000, message = "描述最长 2000 字")
    String description,

    @NotBlank(message = "成色说明不能为空")
    String condition,

    @NotNull(message = "日租金不能为空")
    @DecimalMin(value = "0.01", message = "日租金必须大于 0")
    BigDecimal rentPerDay,

    @NotNull(message = "押金不能为空")
    @DecimalMin(value = "0", message = "押金不能为负")
    BigDecimal deposit,

    Long communityId,

    @Size(max = 128, message = "地点最长 128 字")
    String location,

    List<String> images
) {}
