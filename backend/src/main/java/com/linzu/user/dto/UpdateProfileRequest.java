package com.linzu.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 30, message = "昵称最多 30 个字符") String nickname,
    @Size(max = 255, message = "头像地址过长") String avatarUrl,
    @Size(max = 255, message = "个人简介最多 255 个字符") String bio
) {}
