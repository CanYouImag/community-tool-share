package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveRequest(
        @NotBlank(message = "调解结论不能为空")
        @Size(max = 500, message = "调解结论最多 500 个字符")
        String conclusion
) {
}
