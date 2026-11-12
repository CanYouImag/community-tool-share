package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RenterRespondRequest(
        @NotNull(message = "回应不能为空")
        Boolean agree,

        @Size(max = 500, message = "说明最多 500 个字符")
        String note
) {
}
