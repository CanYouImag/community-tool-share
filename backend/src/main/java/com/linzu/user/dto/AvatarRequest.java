package com.linzu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvatarRequest(
    @NotBlank(message = "头像地址不能为空")
    @Size(max = 255, message = "头像地址过长")
    String avatarUrl
) {}
