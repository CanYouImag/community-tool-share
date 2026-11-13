package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalateRequest(
        @NotBlank(message = "上报说明不能为空")
        @Size(max = 500, message = "上报说明最多 500 个字符")
        String note
) {
}
