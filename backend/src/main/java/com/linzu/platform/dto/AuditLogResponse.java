package com.linzu.platform.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long operatorId,
        String operatorName,
        String action,
        String targetType,
        Long targetId,
        String detail,
        LocalDateTime createdAt
) {
}
