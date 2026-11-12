package com.linzu.user.controller;

import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.storage.FileStorageService;
import com.linzu.user.dto.AvatarRequest;
import com.linzu.user.dto.ChangePasswordRequest;
import com.linzu.user.dto.RealNameRequest;
import com.linzu.user.dto.UpdateProfileRequest;
import com.linzu.user.dto.UserProfileResponse;
import com.linzu.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户")
@RestController
@RequestMapping({ "/api/v1/users", "/api/users" })
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "获取个人资料")
    @GetMapping("/me")
    public Result<UserProfileResponse> me() {
        return Result.ok(userService.getProfile(AuthContext.requireUserId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/me")
    public Result<UserProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(AuthContext.requireUserId(), request);
        return Result.ok();
    }

    @Operation(summary = "提交实名认证")
    @PostMapping("/me/real-name")
    public Result<UserProfileResponse> submitRealName(@Valid @RequestBody RealNameRequest request) {
        return Result.ok(userService.submitRealName(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "修改头像（上传文件）")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserProfileResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(
            userService.updateAvatar(
                AuthContext.requireUserId(),
                fileStorageService.storeAvatar(file)
            )
        );
    }

    @Operation(summary = "修改头像")
    @PutMapping("/me/avatar")
    public Result<UserProfileResponse> updateAvatar(@Valid @RequestBody AvatarRequest request) {
        return Result.ok(
            userService.updateAvatar(AuthContext.requireUserId(), request.avatarUrl())
        );
    }
}
