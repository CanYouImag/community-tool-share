package com.linzu.item.dto;

public record CategoryResponse(
    Long id,
    String name,
    Integer sort,
    String status
) {}
