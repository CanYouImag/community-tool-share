package com.linzu.item.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.RequirePermission;
import com.linzu.item.dto.CategoryRequest;
import com.linzu.item.dto.CategoryResponse;
import com.linzu.item.dto.ItemResponse;
import com.linzu.item.dto.ItemReviewRequest;
import com.linzu.item.dto.ItemViolationRequest;
import com.linzu.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物品平台管理接口：分类维护（C1）、审核（C6）、置顶/违规下架（C7）。
 */
@Tag(name = "物品平台管理")
@RestController
@RequestMapping({ "/api/v1/admin/items", "/api/admin/items" })
@RequiredArgsConstructor
public class ItemAdminController {

    private final ItemService itemService;

    @Operation(summary = "分页查询物品（可按状态筛选，审核队列用 PENDING_REVIEW）")
    @RequirePermission("platform:item-review")
    @GetMapping
    public Result<PageResult<ItemResponse>> page(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(itemService.adminPage(status, page, size));
    }

    @Operation(summary = "审核物品：通过上架 / 驳回（C6）")
    @RequirePermission("platform:item-review")
    @PostMapping("/{id}/review")
    public Result<ItemResponse> review(@PathVariable Long id, @Valid @RequestBody ItemReviewRequest request) {
        return Result.ok(itemService.review(id, request.approved(), request.reason()));
    }

    @Operation(summary = "置顶/取消置顶（C7）")
    @RequirePermission("platform:item-pin")
    @PostMapping("/{id}/pin")
    public Result<Void> pin(@PathVariable Long id, @Valid @RequestBody ItemPinRequest request) {
        itemService.pin(id, Boolean.TRUE.equals(request.pinned()));
        return Result.ok();
    }

    @Operation(summary = "违规下架（C7）")
    @RequirePermission("platform:item-off")
    @PostMapping("/{id}/violation-off")
    public Result<ItemResponse> violationOff(
        @PathVariable Long id,
        @Valid @RequestBody ItemViolationRequest request
    ) {
        return Result.ok(itemService.violationOff(id, request.reason()));
    }

    @Operation(summary = "全部分类（含停用）")
    @RequirePermission("platform:item-review")
    @GetMapping("/categories")
    public Result<List<CategoryResponse>> allCategories() {
        return Result.ok(itemService.listCategories());
    }

    @Operation(summary = "新建分类（C1）")
    @RequirePermission("platform:item-review")
    @PostMapping("/categories")
    public Result<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return Result.ok(itemService.createCategory(request));
    }

    @Operation(summary = "编辑分类（C1）")
    @RequirePermission("platform:item-review")
    @PutMapping("/categories/{id}")
    public Result<CategoryResponse> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody CategoryRequest request
    ) {
        return Result.ok(itemService.updateCategory(id, request));
    }

    public record ItemPinRequest(Boolean pinned) {}
}
