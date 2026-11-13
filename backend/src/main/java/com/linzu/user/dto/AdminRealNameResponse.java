package com.linzu.user.dto;

import java.time.LocalDateTime;

/**
 * 平台管理端实名认证审核列表项。
 */
public record AdminRealNameResponse(
    Long id,
    Long userId,
    String username,
    String nickname,
    String realName,
    String idCardMasked,
    String status,
    String rejectReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
