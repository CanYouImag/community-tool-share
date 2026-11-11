package com.linzu.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReportCreateRequest(
        @NotBlank(message = "举报对象不能为空")
        String target,

        @NotBlank(message = "举报类型不能为空")
        String type,

        Long targetId,

        Long communityId,

        @NotBlank(message = "举报说明不能为空")
        @Size(max = 500, message = "举报说明最多 500 个字符")
        String desc,

        List<@Size(max = 255) String> evidenceUrls
) {
}
