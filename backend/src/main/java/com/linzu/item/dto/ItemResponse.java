package com.linzu.item.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物品列表 / 详情响应。
 * favorited 仅在详情接口返回当前用户视角，列表接口为 null。
 */
public record ItemResponse(
    Long id,
    Long categoryId,
    String categoryName,
    Long ownerId,
    String ownerNickname,
    String ownerAvatar,
    Integer ownerCreditScore,
    String title,
    String description,
    String conditionLevel,
    BigDecimal rentPerDay,
    BigDecimal deposit,
    String location,
    String status,
    Boolean pinned,
    List<String> images,
    Integer viewCount,
    Boolean favorited,
    String rejectReason,
    String violationReason,
    LocalDateTime createdAt
) {}
