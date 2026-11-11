package com.linzu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "原密码不能为空") String oldPassword,
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位")
    String newPassword,
    @NotBlank(message = "确认密码不能为空") String confirmPassword
) {}
