package com.linzu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
    @Pattern(
        regexp = "^[A-Za-z0-9_\\u4e00-\\u9fa5]+$",
        message = "用户名只能包含中文、字母、数字和下划线"
    )
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    String password,

    @NotBlank(message = "确认密码不能为空") String confirmPassword,

    @Size(max = 30, message = "昵称最多 30 个字符") String nickname
) {}
