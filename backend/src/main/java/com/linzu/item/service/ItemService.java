package com.linzu.item.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.community.entity.Community;
import com.linzu.community.entity.CommunityMember;
import com.linzu.community.mapper.CommunityMapper;
import com.linzu.community.mapper.CommunityMemberMapper;
import com.linzu.item.dto.CategoryRequest;
import com.linzu.item.dto.CategoryResponse;
import com.linzu.item.dto.ItemResponse;
import com.linzu.item.dto.ItemSaveRequest;
import com.linzu.item.entity.Item;
import com.linzu.item.entity.ItemCategory;
import com.linzu.item.entity.ItemFavorite;
import com.linzu.item.entity.ItemImage;
import com.linzu.item.enums.ItemStatus;
import com.linzu.item.mapper.ItemCategoryMapper;
import com.linzu.item.mapper.ItemFavoriteMapper;
import com.linzu.item.mapper.ItemImageMapper;
import com.linzu.item.mapper.ItemMapper;
import com.linzu.user.entity.User;
import com.linzu.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 物品域：分类（C1）、搜索筛选（C2）、详情（C3）、收藏（C4）、发布（C5）、
 * 审核（C6）、上下架/置顶/违规下架（C7）、图片。
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final int MAX_IMAGES = 6;

    private final ItemMapper itemMapper;
    private final ItemCategoryMapper categoryMapper;
    private final ItemImageMapper imageMapper;
    private final ItemFavoriteMapper favoriteMapper;
    private final UserService userService;
    private final CommunityMapper communityMapper;
    private final CommunityMemberMapper communityMemberMapper;

    // ==================== 分类（UC-C1） ====================

    public List<CategoryResponse> listCategories() {
        return categoryMapper.selectList(
                Wrappers.<ItemCategory>lambdaQuery()
                    .eq(ItemCategory::getStatus, "ENABLED")
                    .orderByAsc(ItemCategory::getSort)
                    .orderByAsc(ItemCategory::getId)
            )
            .stream()
            .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSort(), c.getStatus()))
            .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        requireCategoryNameFree(request.name(), null);
        ItemCategory category = new ItemCategory();
        category.setName(request.name().trim());
        category.setSort(request.sort() == null ? 0 : request.sort());
        category.setStatus(Boolean.FALSE.equals(request.enabled()) ? "DISABLED" : "ENABLED");
        categoryMapper.insert(category);
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSort(),
            category.getStatus()
        );
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        ItemCategory category = requireCategory(categoryId);
        requireCategoryNameFree(request.name(), categoryId);
        category.setName(request.name().trim());
        if (request.sort() != null) {
            category.setSort(request.sort());
        }
        if (request.enabled() != null) {
            category.setStatus(Boolean.FALSE.equals(request.enabled()) ? "DISABLED" : "ENABLED");
        }
        categoryMapper.updateById(category);
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSort(),
            category.getStatus()
        );
    }

    // ==================== 搜索与筛选（UC-C2） ====================

    public PageResult<ItemResponse> search(
        Long viewerId,
        String keyword,
        Long categoryId,
        Long communityId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort,
        long page,
        long size
    ) {
        if (categoryId != null) {
            requireCategory(categoryId);
        }

        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        List<Long> allowedCommunities = allowedCommunityIds(viewerId);
        if (allowedCommunities != null && allowedCommunities.isEmpty()) {
            return new PageResult<>(List.of(), 0, safePage, safeSize);
        }
        var wrapper = Wrappers.<Item>lambdaQuery()
            .eq(Item::getStatus, ItemStatus.LISTED.name())
            .eq(categoryId != null, Item::getCategoryId, categoryId)
            .eq(communityId != null, Item::getCommunityId, communityId)
            .in(allowedCommunities != null, Item::getCommunityId, allowedCommunities)
            .ge(minPrice != null, Item::getRentPerDay, minPrice)
            .le(maxPrice != null, Item::getRentPerDay, maxPrice)
            .orderByDesc(Item::getIsPinned);
        if (kw != null) {
            wrapper.and(w -> w.like(Item::getTitle, kw).or().like(Item::getDescription, kw));
        }
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Item::getRentPerDay);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Item::getRentPerDay);
        } else if ("hot".equals(sort)) {
            wrapper.orderByDesc(Item::getViewCount);
        }
        wrapper.orderByDesc(Item::getId);

        Page<Item> result = itemMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, String> categoryNames = loadCategoryNames(
            result.getRecords().stream().map(Item::getCategoryId).toList()
        );
        List<ItemResponse> records = result
            .getRecords()
            .stream()
            .map(item -> toResponse(item, categoryNames.get(item.getCategoryId()), null, null, true))
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ==================== 详情（UC-C3） ====================

    public ItemResponse getDetail(Long viewerId, Long itemId) {
        Item item = requireItem(itemId);
        boolean isOwner = viewerId != null && viewerId.equals(item.getOwnerId());
        boolean isAdmin =
            viewerId != null && "PLATFORM_ADMIN".equals(userService.requireUser(viewerId).getRole());
        if (!ItemStatus.LISTED.name().equals(item.getStatus()) && !isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.ITEM_NOT_AVAILABLE);
        }
        if (
            viewerId != null &&
            !isOwner &&
            !isAdmin &&
            item.getCommunityId() != null &&
            !isCommunityMember(viewerId, item.getCommunityId())
        ) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅本社区居民可以查看该物品");
        }

        item.setViewCount(item.getViewCount() == null ? 1 : item.getViewCount() + 1);
        itemMapper.updateById(item);

        ItemCategory category = categoryMapper.selectById(item.getCategoryId());
        User owner = userService.requireUser(item.getOwnerId());
        boolean favorited = viewerId != null && isFavorite(viewerId, itemId);
        return toResponse(
            item,
            category == null ? null : category.getName(),
            owner,
            favorited,
            true
        );
    }

    // ==================== 收藏（UC-C4） ====================

    @Transactional
    public void favorite(Long userId, Long itemId) {
        Item item = requireItem(itemId);
        if (!ItemStatus.LISTED.name().equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.ITEM_NOT_AVAILABLE);
        }
        if (isFavorite(userId, itemId)) {
            return;
        }
        ItemFavorite favorite = new ItemFavorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        favoriteMapper.insert(favorite);
    }

    @Transactional
    public void unfavorite(Long userId, Long itemId) {
        favoriteMapper.delete(
            Wrappers.<ItemFavorite>lambdaQuery()
                .eq(ItemFavorite::getUserId, userId)
                .eq(ItemFavorite::getItemId, itemId)
        );
    }

    public PageResult<ItemResponse> favorites(Long userId, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));

        Page<ItemFavorite> result = favoriteMapper.selectPage(
            new Page<>(safePage, safeSize),
            Wrappers.<ItemFavorite>lambdaQuery()
                .eq(ItemFavorite::getUserId, userId)
                .orderByDesc(ItemFavorite::getId)
        );

        List<Long> itemIds = result.getRecords().stream().map(ItemFavorite::getItemId).toList();
        Map<Long, Item> items = itemIds.isEmpty()
            ? Map.of()
            : itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        Map<Long, String> categoryNames = loadCategoryNames(
            items.values().stream().map(Item::getCategoryId).toList()
        );

        List<ItemResponse> records = result
            .getRecords()
            .stream()
            .filter(f -> items.containsKey(f.getItemId()))
            .map(f -> {
                Item item = items.get(f.getItemId());
                return toResponse(item, categoryNames.get(item.getCategoryId()), null, true, true);
            })
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ==================== 发布（UC-C5） ====================

    @Transactional
    public ItemResponse create(Long ownerId, ItemSaveRequest request) {
        validateSaveRequest(request);
        requireCategory(request.categoryId());
        requireRealName(ownerId);
        requirePublishCredit(ownerId, request.deposit());
        requireCommunityExists(request.communityId());

        Item item = new Item();
        item.setOwnerId(ownerId);
        applySaveRequest(item, request);
        item.setStatus(ItemStatus.PENDING_REVIEW.name());
        item.setIsPinned(0);
        item.setViewCount(0);
        itemMapper.insert(item);
        saveImages(item.getId(), request.images());
        return getDetail(ownerId, item.getId());
    }

    @Transactional
    public ItemResponse update(Long ownerId, Long itemId, ItemSaveRequest request) {
        Item item = requireOwnedItem(ownerId, itemId);
        if (ItemStatus.VIOLATION_OFF.name().equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.ITEM_NOT_AVAILABLE, "违规下架的物品不可编辑");
        }
        validateSaveRequest(request);
        requireCategory(request.categoryId());
        requirePublishCredit(ownerId, request.deposit());
        requireCommunityExists(request.communityId());
        applySaveRequest(item, request);
        // 编辑后需重新审核
        item.setStatus(ItemStatus.PENDING_REVIEW.name());
        item.setRejectReason(null);
        itemMapper.updateById(item);
        imageMapper.delete(Wrappers.<ItemImage>lambdaQuery().eq(ItemImage::getItemId, itemId));
        saveImages(itemId, request.images());
        return getDetail(ownerId, itemId);
    }

    @Transactional
    public void delete(Long ownerId, Long itemId) {
        requireOwnedItem(ownerId, itemId);
        itemMapper.deleteById(itemId);
    }

    public PageResult<ItemResponse> myItems(Long ownerId, String status, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        Page<Item> result = itemMapper.selectPage(
            new Page<>(safePage, safeSize),
            Wrappers.<Item>lambdaQuery()
                .eq(Item::getOwnerId, ownerId)
                .eq(StringUtils.hasText(status), Item::getStatus, status)
                .orderByDesc(Item::getId)
        );
        Map<Long, String> categoryNames = loadCategoryNames(
            result.getRecords().stream().map(Item::getCategoryId).toList()
        );
        List<ItemResponse> records = result
            .getRecords()
            .stream()
            .map(item -> toResponse(item, categoryNames.get(item.getCategoryId()), null, null, true))
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ==================== 上下架 / 置顶 / 违规下架（UC-C7） ====================

    @Transactional
    public void shelf(Long ownerId, Long itemId, boolean onShelf) {
        Item item = requireOwnedItem(ownerId, itemId);
        String current = item.getStatus();
        if (onShelf) {
            if (!ItemStatus.DELISTED.name().equals(current)) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "仅已下架的物品可重新上架");
            }
            item.setStatus(ItemStatus.LISTED.name());
        } else {
            if (!ItemStatus.LISTED.name().equals(current)) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "仅上架中的物品可下架");
            }
            item.setStatus(ItemStatus.DELISTED.name());
        }
        itemMapper.updateById(item);
    }

    // ==================== 平台审核（UC-C6） ====================

    public PageResult<ItemResponse> adminPage(String status, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        Page<Item> result = itemMapper.selectPage(
            new Page<>(safePage, safeSize),
            Wrappers.<Item>lambdaQuery()
                .eq(StringUtils.hasText(status), Item::getStatus, status)
                .orderByDesc(Item::getId)
        );
        Map<Long, String> categoryNames = loadCategoryNames(
            result.getRecords().stream().map(Item::getCategoryId).toList()
        );
        List<ItemResponse> records = result
            .getRecords()
            .stream()
            .map(item -> toResponse(item, categoryNames.get(item.getCategoryId()), null, null, true))
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public ItemResponse review(Long itemId, boolean approved, String reason) {
        Item item = requireItem(itemId);
        if (!ItemStatus.PENDING_REVIEW.name().equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "物品不在待审核状态");
        }
        if (approved) {
            item.setStatus(ItemStatus.LISTED.name());
            item.setRejectReason(null);
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回必须填写原因");
            }
            item.setStatus(ItemStatus.REJECTED.name());
            item.setRejectReason(reason.trim());
        }
        itemMapper.updateById(item);
        return getDetail(item.getOwnerId(), itemId);
    }

    @Transactional
    public void pin(Long itemId, boolean pinned) {
        Item item = requireItem(itemId);
        if (pinned && (item.getIsPinned() == null || item.getIsPinned() == 0)) {
            Long pinnedCount = itemMapper.selectCount(
                Wrappers
                    .<Item>lambdaQuery()
                    .eq(Item::getCommunityId, item.getCommunityId())
                    .eq(Item::getIsPinned, 1)
            );
            if (pinnedCount != null && pinnedCount >= 3) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "每个社区最多置顶 3 件物品");
            }
        }
        item.setIsPinned(Boolean.TRUE.equals(pinned) ? 1 : 0);
        itemMapper.updateById(item);
    }

    @Transactional
    public ItemResponse violationOff(Long itemId, String reason) {
        Item item = requireItem(itemId);
        if (ItemStatus.VIOLATION_OFF.name().equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "物品已违规下架");
        }
        item.setStatus(ItemStatus.VIOLATION_OFF.name());
        item.setViolationReason(StringUtils.hasText(reason) ? reason.trim() : "违反平台规则");
        item.setIsPinned(0);
        itemMapper.updateById(item);
        return getDetail(item.getOwnerId(), itemId);
    }

    // ==================== 内部辅助 ====================

    public Item requireItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        return item;
    }

    private Item requireOwnedItem(Long ownerId, Long itemId) {
        Item item = requireItem(itemId);
        if (!ownerId.equals(item.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return item;
    }

    private ItemCategory requireCategory(Long categoryId) {
        ItemCategory category = categoryMapper.selectById(categoryId);
        if (category == null || "DISABLED".equals(category.getStatus())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private void requireCategoryNameFree(String name, Long excludeId) {
        Long count = categoryMapper.selectCount(
            Wrappers.<ItemCategory>lambdaQuery()
                .eq(ItemCategory::getName, name.trim())
                .ne(excludeId != null, ItemCategory::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXISTS);
        }
    }

    private void validateSaveRequest(ItemSaveRequest request) {
        List<String> images = request.images();
        if (images == null || images.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发布物品至少需要上传 1 张图片");
        }
        if (images.size() > MAX_IMAGES) {
            throw new BusinessException(ErrorCode.ITEM_IMAGE_TOO_MANY);
        }
    }

    private void requireRealName(Long ownerId) {
        var profile = userService.getProfile(ownerId);
        if (!"VERIFIED".equals(profile.realNameStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "发布物品前请先完成实名认证");
        }
    }

    private void requirePublishCredit(Long ownerId, BigDecimal deposit) {
        if (deposit == null || deposit.compareTo(new BigDecimal("500")) <= 0) {
            return;
        }
        User owner = userService.requireUser(ownerId);
        if (owner.getCreditScore() == null || owner.getCreditScore() < 60) {
            throw new BusinessException(
                ErrorCode.PARAM_ERROR,
                "信用分不足 60，无法发布押金超过 500 元的物品"
            );
        }
    }

    private void requireCommunityExists(Long communityId) {
        if (communityId != null && communityMapper.selectById(communityId) == null) {
            throw new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND);
        }
    }

    private void applySaveRequest(Item item, ItemSaveRequest request) {
        item.setCategoryId(request.categoryId());
        item.setTitle(request.title().trim());
        item.setDescription(
            StringUtils.hasText(request.description()) ? request.description().trim() : null
        );
        item.setConditionDesc(request.condition().trim());
        item.setRentPerDay(request.rentPerDay().setScale(2, RoundingMode.HALF_UP));
        item.setDeposit(request.deposit().setScale(2, RoundingMode.HALF_UP));
        item.setCommunityId(request.communityId());
        item.setLocation(request.location() == null ? null : request.location().trim());
    }

    private void saveImages(Long itemId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int i = 0; i < images.size(); i++) {
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(images.get(i));
            image.setSort(i);
            imageMapper.insert(image);
        }
    }

    private String resolveCommunityName(Long communityId) {
        if (communityId == null) {
            return null;
        }
        Community community = communityMapper.selectById(communityId);
        return community == null ? null : community.getName();
    }

    private List<Long> allowedCommunityIds(Long viewerId) {
        if (viewerId == null) {
            return null;
        }
        User viewer = userService.requireUser(viewerId);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) {
            return null;
        }
        return communityMemberMapper
            .selectList(
                Wrappers
                    .<CommunityMember>lambdaQuery()
                    .eq(CommunityMember::getUserId, viewerId)
                    .eq(CommunityMember::getStatus, "ACTIVE")
            )
            .stream()
            .map(CommunityMember::getCommunityId)
            .distinct()
            .toList();
    }

    private boolean isCommunityMember(Long userId, Long communityId) {
        Long count = communityMemberMapper.selectCount(
            Wrappers
                .<CommunityMember>lambdaQuery()
                .eq(CommunityMember::getUserId, userId)
                .eq(CommunityMember::getCommunityId, communityId)
                .eq(CommunityMember::getStatus, "ACTIVE")
        );
        return count != null && count > 0;
    }

    private boolean isFavorite(Long userId, Long itemId) {
        return favoriteMapper.selectCount(
            Wrappers.<ItemFavorite>lambdaQuery()
                .eq(ItemFavorite::getUserId, userId)
                .eq(ItemFavorite::getItemId, itemId)
        ) > 0;
    }

    private Map<Long, String> loadCategoryNames(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper
            .selectBatchIds(categoryIds.stream().distinct().toList())
            .stream()
            .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName));
    }

    private ItemResponse toResponse(
        Item item,
        String categoryName,
        User owner,
        Boolean favorited,
        boolean withImages
    ) {
        List<String> images = withImages
            ? imageMapper
                .selectList(
                    Wrappers.<ItemImage>lambdaQuery()
                        .eq(ItemImage::getItemId, item.getId())
                        .orderByAsc(ItemImage::getSort)
                        .orderByAsc(ItemImage::getId)
                )
                .stream()
                .map(ItemImage::getUrl)
                .toList()
            : List.of();
        return new ItemResponse(
            item.getId(),
            item.getCategoryId(),
            categoryName,
            item.getOwnerId(),
            owner == null ? null : owner.getNickname(),
            owner == null ? null : owner.getAvatarUrl(),
            owner == null ? null : owner.getCreditScore(),
            item.getCommunityId(),
            resolveCommunityName(item.getCommunityId()),
            item.getTitle(),
            item.getDescription(),
            item.getConditionDesc(),
            item.getRentPerDay(),
            item.getDeposit(),
            item.getLocation(),
            item.getStatus(),
            item.getIsPinned() != null && item.getIsPinned() == 1,
            images,
            item.getViewCount(),
            favorited,
            item.getRejectReason(),
            item.getViolationReason(),
            item.getCreatedAt()
        );
    }
}
