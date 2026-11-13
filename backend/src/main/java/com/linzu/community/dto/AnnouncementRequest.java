package com.linzu.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 128, message = "标题最多 128 个字符")
        String title,

        @NotBlank(message = "正文不能为空")
        String content,

        @Size(max = 255, message = "配图地址最多 255 个字符")
        String imageUrl
) {
}
