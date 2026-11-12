package com.linzu.user.dto;

/**
 * 图形验证码。image 为可直接用于 img src 的 data URI。
 */
public record CaptchaResponse(String captchaId, String image, long expireSeconds) {}
