package com.linzu.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditJoinApplyRequest(
        @NotNull(message = "审核结果不能为空")
        Boolean approve,

        @Size(max = 255, message = "拒绝原因最多 255 个字符")
        String rejectReason
) {
}
