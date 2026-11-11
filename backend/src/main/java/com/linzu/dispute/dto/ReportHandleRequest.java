package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportHandleRequest(
        @NotBlank(message = "处理措施不能为空")
        String action,

        @Size(max = 500, message = "处理意见最多 500 个字符")
        String note
) {
}
