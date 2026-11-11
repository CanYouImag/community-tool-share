package com.linzu.platform.dto;

import jakarta.validation.constraints.Size;

public record BanUserRequest(
        @Size(max = 500, message = "原因最多 500 个字符")
        String reason
) {
}
