package com.linzu.item.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 物品上下架（UC-C7）：onShelf true 上架 false 下架
 */
public record ItemShelfRequest(
    @NotNull(message = "上架状态不能为空")
    Boolean onShelf
) {}
