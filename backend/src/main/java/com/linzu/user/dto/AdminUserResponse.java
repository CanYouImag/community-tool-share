package com.linzu.user.dto;

import java.time.LocalDateTime;

/**
 * 平台管理端用户列表项。身份证号只返回脱敏值，不返回密文。
 */
public record AdminUserResponse(
    Long id,
    String username,
    String phone,
    String nickname,
    String avatarUrl,
    String role,
    String status,
    Integer creditScore,
    String realNameStatus,
    String realName,
    String idCardMasked,
    String rejectReason,
    LocalDateTime createdAt
) {}
