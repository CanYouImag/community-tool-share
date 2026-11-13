package com.linzu.dispute.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.PermissionCodes;
import com.linzu.common.security.RequirePermission;
import com.linzu.dispute.dto.ReportCreateRequest;
import com.linzu.dispute.dto.ReportHandleRequest;
import com.linzu.dispute.dto.ReportResponse;
import com.linzu.dispute.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "举报")
@RestController
@RequestMapping({"/api/v1/reports", "/api/reports"})
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "提交举报（UC-F7）")
    @PostMapping
    public Result<ReportResponse> create(@Valid @RequestBody ReportCreateRequest request) {
        return Result.ok(reportService.create(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "我的举报")
    @GetMapping("/mine")
    public Result<List<ReportResponse>> mine() {
        return Result.ok(reportService.listMine(AuthContext.requireUserId()));
    }

    @Operation(summary = "举报处理队列（UC-F8）")
    @GetMapping
    @RequirePermission(PermissionCodes.COMMUNITY_REPORT_HANDLE)
    public Result<PageResult<ReportResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.ok(reportService.listForAdmin(AuthContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "处理举报（UC-F8）")
    @PostMapping("/{id}/handle")
    @RequirePermission(PermissionCodes.COMMUNITY_REPORT_HANDLE)
    public Result<ReportResponse> handle(
            @PathVariable Long id,
            @Valid @RequestBody ReportHandleRequest request
    ) {
        return Result.ok(reportService.handle(AuthContext.requireUserId(), id, request));
    }
}
