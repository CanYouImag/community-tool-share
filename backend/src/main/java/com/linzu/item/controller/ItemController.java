package com.linzu.item.controller;

import com.linzu.common.api.PageResult;
import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.common.storage.FileStorageService;
import com.linzu.item.dto.CategoryResponse;
import com.linzu.item.dto.ItemResponse;
import com.linzu.item.dto.ItemSaveRequest;
import com.linzu.item.dto.ItemShelfRequest;
import com.linzu.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 物品域接口：搜索筛选（C2）、详情（C3）、收藏（C4）、发布（C5）、上下架（C7）、图片上传。
 */
@Tag(name = "物品")
@RestController
@RequestMapping({ "/api/v1/items", "/api/items" })
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "分类列表（C1）")
    @GetMapping("/categories")
    public Result<List<CategoryResponse>> categories() {
        return Result.ok(itemService.listCategories());
    }

    @Operation(summary = "物品图片上传")
    @PostMapping("/images")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file, "items");
        return Result.ok(Map.of("url", url));
    }

    @Operation(summary = "搜索与筛选物品（C2），sort: newest/price_asc/price_desc/hot")
    @GetMapping
    public Result<PageResult<ItemResponse>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String condition,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "newest") String sort,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(
            itemService.search(
                AuthContext.requireUserId(),
                keyword,
                categoryId,
                condition,
                minPrice,
                maxPrice,
                sort,
                page,
                size
            )
        );
    }

    @Operation(summary = "物品详情（C3）")
    @GetMapping("/{id}")
    public Result<ItemResponse> detail(@PathVariable Long id) {
        return Result.ok(itemService.getDetail(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "发布物品（C5），提交后进入平台审核")
    @PostMapping
    public Result<ItemResponse> create(@Valid @RequestBody ItemSaveRequest request) {
        return Result.ok(itemService.create(AuthContext.requireUserId(), request));
    }

    @Operation(summary = "编辑物品，编辑后需重新审核")
    @PutMapping("/{id}")
    public Result<ItemResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody ItemSaveRequest request
    ) {
        return Result.ok(itemService.update(AuthContext.requireUserId(), id, request));
    }

    @Operation(summary = "删除物品（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        itemService.delete(AuthContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "我的物品（全部状态）")
    @GetMapping("/mine")
    public Result<PageResult<ItemResponse>> mine(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(itemService.myItems(AuthContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "上架/下架（C7）")
    @PutMapping("/{id}/shelf")
    public Result<Void> shelf(@PathVariable Long id, @Valid @RequestBody ItemShelfRequest request) {
        itemService.shelf(AuthContext.requireUserId(), id, Boolean.TRUE.equals(request.onShelf()));
        return Result.ok();
    }

    @Operation(summary = "收藏物品（C4）")
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        itemService.favorite(AuthContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "取消收藏（C4）")
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        itemService.unfavorite(AuthContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "我的收藏（C4）")
    @GetMapping("/favorites")
    public Result<PageResult<ItemResponse>> favorites(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.ok(itemService.favorites(AuthContext.requireUserId(), page, size));
    }
}
