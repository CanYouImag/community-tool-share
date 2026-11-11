package com.linzu.dispute.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        Long reporterId,
        String reporterName,
        Long communityId,
        String targetType,
        Long targetId,
        String reasonType,
        String description,
        List<String> evidenceUrls,
        String status,
        String handleAction,
        String handleNote,
        Long handledBy,
        String handlerName,
        LocalDateTime handledAt,
        LocalDateTime createdAt
) {
}
