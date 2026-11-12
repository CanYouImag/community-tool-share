package com.linzu.dispute.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.PermissionCodes;
import com.linzu.common.security.RequirePermission;
import com.linzu.dispute.dto.ArbitrateRequest;
import com.linzu.dispute.dto.AwardResponse;
import com.linzu.dispute.dto.DisputeCreateRequest;
import com.linzu.dispute.dto.DisputeDetailResponse;
import com.linzu.dispute.dto.DisputeMessageResponse;
import com.linzu.dispute.dto.DisputeResponse;
import com.linzu.dispute.dto.EscalateRequest;
import com.linzu.dispute.dto.MediateRequest;
import com.linzu.dispute.dto.RenterRespondRequest;
import com.linzu.dispute.dto.ResolveRequest;
import com.linzu.dispute.service.DisputeService;
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

@Tag(name = "纠纷")
@RestController
@RequestMapping({"/api/v1/disputes", "/api/disputes"})
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @Operation(summary = "物主提出扣留押金（UC-F1）")
    @PostMapping
    public Result<DisputeResponse> create(@Valid @RequestBody DisputeCreateRequest request) {
        return Result.ok(disputeService.create(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "租客回应物主扣留押金（同意/不同意）")
    @PostMapping("/{id}/respond")
    public Result<DisputeResponse> respond(@PathVariable Long id, @Valid @RequestBody RenterRespondRequest request) {
        return Result.ok(disputeService.respond(AuthContext.requireUserId(), id, request));
    }

    @Operation(summary = "我的纠纷（用户视角）")
    @GetMapping("/mine")
    public Result<List<DisputeResponse>> mine() {
        return Result.ok(disputeService.listMine(AuthContext.requireUserId()));
    }

    @Operation(summary = "纠纷队列（调解工作台 / 仲裁中心）")
    @GetMapping
    @RequirePermission(PermissionCodes.COMMUNITY_MEDIATE)
    public Result<PageResult<DisputeResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.ok(disputeService.listForAdmin(AuthContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "纠纷详情（当事人 / 管理员）")
    @GetMapping("/{id}")
    public Result<DisputeDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(disputeService.detail(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "调解留言（UC-F2）")
    @PostMapping("/{id}/messages")
    @RequirePermission(PermissionCodes.COMMUNITY_MEDIATE)
    public Result<DisputeMessageResponse> mediate(
            @PathVariable Long id,
            @Valid @RequestBody MediateRequest request
    ) {
        return Result.ok(disputeService.mediate(AuthContext.requireUserId(), id, request));
    }

    @Operation(summary = "标记调解成功（UC-F3）")
    @PostMapping("/{id}/resolve")
    @RequirePermission(PermissionCodes.COMMUNITY_MEDIATE)
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest request) {
        disputeService.resolve(AuthContext.requireUserId(), id, request);
        return Result.ok();
    }

    @Operation(summary = "上报仲裁（UC-F4）")
    @PostMapping("/{id}/escalate")
    @RequirePermission(PermissionCodes.COMMUNITY_REPORT_ESCALATE)
    public Result<Void> escalate(@PathVariable Long id, @Valid @RequestBody EscalateRequest request) {
        disputeService.escalate(AuthContext.requireUserId(), id, request);
        return Result.ok();
    }

    @Operation(summary = "仲裁裁决并划转押金（UC-F5/F6）")
    @PostMapping("/{id}/arbitrate")
    @RequirePermission(PermissionCodes.PLATFORM_ARBITRATE)
    public Result<AwardResponse> arbitrate(
            @PathVariable Long id,
            @Valid @RequestBody ArbitrateRequest request
    ) {
        return Result.ok(disputeService.arbitrate(AuthContext.requireUserId(), id, request));
    }

    @Operation(summary = "裁决书（UC-F6，双方可见）")
    @GetMapping("/{id}/award")
    public Result<AwardResponse> award(@PathVariable Long id) {
        return Result.ok(disputeService.getAward(AuthContext.requireUserId(), id));
    }
}
