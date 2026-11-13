package com.linzu.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityUpdateRequest(
        @NotBlank(message = "社区名称不能为空")
        @Size(max = 64, message = "社区名称最多 64 个字符")
        String name,

        @Size(max = 255, message = "社区简介最多 255 个字符")
        String intro,

        @Size(max = 255)
        String coverUrl,

        Boolean autoApprove,

        String status
) {
}
