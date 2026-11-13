package com.linzu.community.dto;

/**
 * 社区卡片/列表响应。字段命名对齐前端 CommunityCard.vue / CommunityView.vue。
 */
public record CommunityResponse(
        Long id,
        String name,
        String intro,
        String coverUrl,
        boolean autoApprove,
        int members,
        int onRent,
        String admin,
        String notice
) {
}
