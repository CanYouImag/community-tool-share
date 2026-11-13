package com.linzu.platform.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.PermissionCodes;
import com.linzu.common.security.RequirePermission;
import com.linzu.platform.dto.AppointAdminRequest;
import com.linzu.platform.dto.AuditLogResponse;
import com.linzu.platform.dto.CommunityAdminResponse;
import com.linzu.platform.dto.CommunityCreateRequest;
import com.linzu.platform.dto.CommunityUpdateRequest;
import com.linzu.platform.dto.ConfigUpdateRequest;
import com.linzu.platform.service.AuditLogService;
import com.linzu.platform.service.PlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "平台治理")
@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;
    private final AuditLogService auditLogService;

    // ===== UC-G1 社区 CRUD =====

    @Operation(summary = "社区管理列表（含停用）")
    @GetMapping("/communities")
    @RequirePermission(PermissionCodes.PLATFORM_COMMUNITY)
    public Result<List<CommunityAdminResponse>> communities(@RequestParam(required = false) String keyword) {
        return Result.ok(platformService.listCommunities(keyword));
    }

    @Operation(summary = "创建社区（UC-G1）")
    @PostMapping("/communities")
    @RequirePermission(PermissionCodes.PLATFORM_COMMUNITY)
    public Result<CommunityAdminResponse> createCommunity(@Valid @RequestBody CommunityCreateRequest request) {
        return Result.ok(platformService.createCommunity(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "编辑社区（UC-G1）")
    @PutMapping("/communities/{id}")
    @RequirePermission(PermissionCodes.PLATFORM_COMMUNITY)
    public Result<CommunityAdminResponse> updateCommunity(
            @PathVariable Long id,
            @Valid @RequestBody CommunityUpdateRequest request
    ) {
        return Result.ok(platformService.updateCommunity(AuthContext.requireUserId(), id, request));
    }

    // ===== UC-G2 任命/罢免社区管理员 =====

    @Operation(summary = "任命社区管理员（UC-G2）")
    @PostMapping("/communities/{id}/admin")
    @RequirePermission(PermissionCodes.PLATFORM_ADMIN_APPOINT)
    public Result<CommunityAdminResponse> appointAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AppointAdminRequest request
    ) {
        return Result.ok(platformService.appointAdmin(AuthContext.requireUserId(), id, request.userId()));
    }

    @Operation(summary = "罢免社区管理员（UC-G2）")
    @DeleteMapping("/communities/{id}/admin/{userId}")
    @RequirePermission(PermissionCodes.PLATFORM_ADMIN_APPOINT)
    public Result<CommunityAdminResponse> dismissAdmin(@PathVariable Long id, @PathVariable Long userId) {
        return Result.ok(platformService.dismissAdmin(AuthContext.requireUserId(), id, userId));
    }

    // UC-G3 用户管理由 AdminUserController 统一提供（含用户搜索、封禁/解封、实名审核）。

    // ===== UC-G4 平台参数配置 =====

    @Operation(summary = "读取平台参数（UC-G4）")
    @GetMapping("/config")
    @RequirePermission(PermissionCodes.PLATFORM_CONFIG)
    public Result<Map<String, Object>> getConfig() {
        return Result.ok(platformService.getConfig());
    }

    @Operation(summary = "更新平台参数（UC-G4）")
    @PutMapping("/config")
    @RequirePermission(PermissionCodes.PLATFORM_CONFIG)
    public Result<Map<String, Object>> updateConfig(@Valid @RequestBody ConfigUpdateRequest request) {
        return Result.ok(platformService.updateConfig(AuthContext.requireUserId(), request));
    }

    // ===== UC-G5 / UC-G7 数据统计 =====

    @Operation(summary = "全平台数据统计（UC-G5）")
    @GetMapping("/stats")
    @RequirePermission(PermissionCodes.PLATFORM_STATS)
    public Result<Map<String, Object>> platformStats() {
        return Result.ok(platformService.platformStats());
    }

    @Operation(summary = "本社区数据统计（UC-G7）")
    @GetMapping("/community-stats")
    @RequirePermission(PermissionCodes.COMMUNITY_STATS)
    public Result<Map<String, Object>> communityStats() {
        return Result.ok(platformService.communityStats(AuthContext.requireUserId()));
    }

    // ===== UC-G6 操作日志 =====

    @Operation(summary = "查询操作日志（UC-G6）")
    @GetMapping("/logs")
    @RequirePermission(PermissionCodes.PLATFORM_LOG)
    public Result<PageResult<AuditLogResponse>> logs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.ok(auditLogService.list(action, operatorId, page, size));
    }
}
