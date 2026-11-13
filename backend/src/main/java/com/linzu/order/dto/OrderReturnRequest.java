package com.linzu.order.dto;

import java.util.List;

/**
 * 归还请求（UC-D5）：至少上传 1 张归还凭证。
 */
public record OrderReturnRequest(List<String> photoUrls) {}
