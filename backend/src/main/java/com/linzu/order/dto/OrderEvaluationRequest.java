package com.linzu.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderEvaluationRequest(
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    Integer rating,
    @Size(max = 500, message = "评价内容最多 500 字")
    String content,
    List<String> imageUrls
) {}
