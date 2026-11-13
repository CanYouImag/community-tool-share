package com.linzu.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类创建 / 编辑
 */
public record CategoryRequest(
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称最长 32 字")
    String name,

    Integer sort,

    Boolean enabled
) {}
