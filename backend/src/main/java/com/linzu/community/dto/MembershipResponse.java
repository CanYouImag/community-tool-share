package com.linzu.community.dto;

/**
 * 我的社区成员关系，对齐前端 memberships 列表：{ id, name, status, time }。
 * status: JOINED / PENDING
 */
public record MembershipResponse(
        Long id,
        String name,
        String status,
        String time
) {
}
