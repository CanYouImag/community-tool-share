package com.linzu.user.controller;

import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.user.dto.LoginRequest;
import com.linzu.user.dto.LoginResponse;
import com.linzu.user.dto.RegisterRequest;
import com.linzu.user.dto.UserProfileResponse;
import com.linzu.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserProfileResponse> me() {
        return Result.ok(authService.me(AuthContext.requireUserId()));
    }

    @Operation(summary = "退出登录（前端清除 token）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
