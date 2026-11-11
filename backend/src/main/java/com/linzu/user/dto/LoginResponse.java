package com.linzu.user.dto;

public record LoginResponse(String token, long expiresIn, UserProfileResponse user) {}
