package com.linzu.order.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 取物请求（UC-D4）：必须提供取物码，并至少上传 1 张取物凭证。
 */
public record OrderPickupRequest(
    @NotBlank(message = "取物码不能为空")
    String code,
    List<String> photoUrls
) {}
