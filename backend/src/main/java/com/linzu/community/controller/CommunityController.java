package com.linzu.community.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.PermissionCodes;
import com.linzu.common.security.RequirePermission;
import com.linzu.community.dto.AnnouncementRequest;
import com.linzu.community.dto.AnnouncementResponse;
import com.linzu.community.dto.AuditJoinApplyRequest;
import com.linzu.community.dto.CommunityMemberResponse;
import com.linzu.community.dto.CommunityResponse;
import com.linzu.community.dto.JoinApplyResponse;
import com.linzu.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "社区")
@RestController
@RequestMapping({"/api/v1/communities", "/api/communities"})
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "社区列表（游客可浏览）")
    @GetMapping
    public Result<List<CommunityResponse>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(communityService.list(keyword));
    }

    @Operation(summary = "社区主页（游客可浏览）")
    @GetMapping("/home/{id}")
    public Result<Map<String, Object>> home(@PathVariable Long id) {
        return Result.ok(communityService.home(id));
    }

    @Operation(summary = "申请加入社区")
    @PostMapping("/{id}/join")
    public Result<Map<String, Object>> join(@PathVariable Long id) {
        return Result.ok(communityService.join(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "退出社区")
    @PostMapping("/{id}/quit")
    public Result<Void> quit(@PathVariable Long id) {
        communityService.quit(AuthContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "社区成员列表（社区管理员）")
    @GetMapping("/{id}/members")
    @RequirePermission(PermissionCodes.COMMUNITY_MEMBER_MANAGE)
    public Result<PageResult<CommunityMemberResponse>> members(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.ok(communityService.listMembers(AuthContext.requireUserId(), id, page, size));
    }

    @Operation(summary = "踢出社区成员（社区管理员）")
    @DeleteMapping("/{id}/members/{userId}")
    @RequirePermission(PermissionCodes.COMMUNITY_MEMBER_MANAGE)
    public Result<Void> kick(@PathVariable Long id, @PathVariable Long userId) {
        communityService.kick(AuthContext.requireUserId(), id, userId);
        return Result.ok();
    }

    @Operation(summary = "加入申请列表（社区管理员）")
    @GetMapping("/{id}/join-applies")
    @RequirePermission(PermissionCodes.COMMUNITY_JOIN_AUDIT)
    public Result<PageResult<JoinApplyResponse>> joinApplies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status
    ) {
        return Result.ok(communityService.listJoinApplies(AuthContext.requireUserId(), id, page, size, status));
    }

    @Operation(summary = "审核加入申请（社区管理员）")
    @PostMapping("/{id}/join-applies/{applyId}/audit")
    @RequirePermission(PermissionCodes.COMMUNITY_JOIN_AUDIT)
    public Result<Void> auditJoin(
            @PathVariable Long id,
            @PathVariable Long applyId,
            @Valid @RequestBody AuditJoinApplyRequest request
    ) {
        communityService.auditJoin(AuthContext.requireUserId(), id, applyId, request);
        return Result.ok();
    }

    @Operation(summary = "社区公告列表（游客可浏览）")
    @GetMapping("/{id}/announcements")
    public Result<List<AnnouncementResponse>> announcements(@PathVariable Long id) {
        return Result.ok(communityService.listAnnouncements(id));
    }

    @Operation(summary = "发布社区公告（社区管理员）")
    @PostMapping("/{id}/announcements")
    @RequirePermission(PermissionCodes.COMMUNITY_ANNOUNCE)
    public Result<AnnouncementResponse> publishAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return Result.ok(communityService.publishAnnouncement(AuthContext.requireUserId(), id, request));
    }

    @Operation(summary = "删除社区公告（社区管理员）")
    @DeleteMapping("/{id}/announcements/{announcementId}")
    @RequirePermission(PermissionCodes.COMMUNITY_ANNOUNCE)
    public Result<Void> deleteAnnouncement(@PathVariable Long id, @PathVariable Long announcementId) {
        communityService.deleteAnnouncement(AuthContext.requireUserId(), id, announcementId);
        return Result.ok();
    }
}
