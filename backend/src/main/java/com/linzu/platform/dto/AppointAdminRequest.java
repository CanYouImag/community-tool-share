package com.linzu.platform.dto;

import jakarta.validation.constraints.NotNull;

public record AppointAdminRequest(
        @NotNull(message = "目标用户不能为空")
        Long userId
) {
}
