package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MediateRequest(
        @NotBlank(message = "调解留言不能为空")
        @Size(max = 500, message = "留言最多 500 个字符")
        String content
) {
}
