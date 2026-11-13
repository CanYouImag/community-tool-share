package com.linzu.user.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.PermissionCodes;
import com.linzu.common.security.RequirePermission;
import com.linzu.platform.service.AuditLogService;
import com.linzu.user.dto.AdminRealNameResponse;
import com.linzu.user.dto.AdminUserResponse;
import com.linzu.user.dto.RealNameReviewRequest;
import com.linzu.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台管理端用户与实名认证审核接口。
 *
 * <p>权限码与钟姝伶维护的 {@link PermissionCodes} 对齐：
 * 用户管理使用 platform:ban，实名审核使用 platform:cert。</p>
 */
@Tag(name = "平台用户管理")
@RestController
@RequestMapping({ "/api/v1/admin", "/api/admin" })
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询用户（支持用户名/昵称/手机号搜索、角色和状态筛选）")
    @RequirePermission(PermissionCodes.PLATFORM_BAN)
    @GetMapping("/users")
    public Result<PageResult<AdminUserResponse>> users(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(adminUserService.listUsers(keyword, role, status, page, size));
    }

    @Operation(summary = "封禁用户")
    @RequirePermission(PermissionCodes.PLATFORM_BAN)
    @PostMapping("/users/{id}/ban")
    public Result<AdminUserResponse> ban(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        Long operatorId = AuthContext.requireUserId();
        AdminUserResponse response = adminUserService.banUser(operatorId, id);
        auditLogService.record(operatorId, "BAN_USER", "USER", id, reason(body));
        return Result.ok(response);
    }

    @Operation(summary = "解封用户")
    @RequirePermission(PermissionCodes.PLATFORM_BAN)
    @PostMapping("/users/{id}/unban")
    public Result<AdminUserResponse> unban(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body
    ) {
        Long operatorId = AuthContext.requireUserId();
        AdminUserResponse response = adminUserService.unbanUser(id);
        auditLogService.record(operatorId, "UNBAN_USER", "USER", id, reason(body));
        return Result.ok(response);
    }

    @Operation(summary = "分页查询实名认证申请（审核队列可传 status=PENDING）")
    @RequirePermission(PermissionCodes.PLATFORM_CERT)
    @GetMapping("/real-names")
    public Result<PageResult<AdminRealNameResponse>> realNames(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(adminUserService.listRealNames(status, keyword, page, size));
    }

    @Operation(summary = "通过实名认证")
    @RequirePermission(PermissionCodes.PLATFORM_CERT)
    @PostMapping("/real-names/{id}/approve")
    public Result<AdminRealNameResponse> approveRealName(@PathVariable Long id) {
        return Result.ok(adminUserService.approveRealName(id));
    }

    @Operation(summary = "驳回实名认证")
    @RequirePermission(PermissionCodes.PLATFORM_CERT)
    @PostMapping("/real-names/{id}/reject")
    public Result<AdminRealNameResponse> rejectRealName(
        @PathVariable Long id,
        @Valid @RequestBody RealNameReviewRequest request
    ) {
        return Result.ok(adminUserService.rejectRealName(id, request.reason()));
    }

    private String reason(Map<String, String> body) {
        return body == null ? null : body.get("reason");
    }
}
