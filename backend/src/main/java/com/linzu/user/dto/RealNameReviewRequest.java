package com.linzu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员驳回实名认证时填写的原因。
 */
public record RealNameReviewRequest(
    @NotBlank(message = "驳回理由不能为空")
    @Size(max = 255, message = "驳回理由不能超过 255 个字符")
    String reason
) {}
