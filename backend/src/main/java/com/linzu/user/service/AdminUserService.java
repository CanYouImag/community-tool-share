package com.linzu.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.common.security.IdCardCipher;
import com.linzu.common.util.IdCardUtil;
import com.linzu.user.dto.AdminRealNameResponse;
import com.linzu.user.dto.AdminUserResponse;
import com.linzu.user.entity.User;
import com.linzu.user.entity.UserRealName;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.mapper.UserRealNameMapper;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 平台管理端：用户列表、封禁/解封、实名认证审核。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String UNVERIFIED = "UNVERIFIED";
    private static final String PENDING = "PENDING";
    private static final String VERIFIED = "VERIFIED";
    private static final String REJECTED = "REJECTED";
    private static final String ACTIVE = "ACTIVE";
    private static final String BANNED = "BANNED";
    private static final String CERTIFIED = "CERTIFIED";
    private static final String COMMUNITY_ADMIN = "COMMUNITY_ADMIN";
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final UserMapper userMapper;
    private final UserRealNameMapper userRealNameMapper;
    private final IdCardCipher idCardCipher;

    public PageResult<AdminUserResponse> listUsers(
        String keyword,
        String role,
        String status,
        long pageNo,
        long pageSize
    ) {
        long safePage = Math.max(1, pageNo);
        long safeSize = Math.min(100, Math.max(1, pageSize));
        String kw = normalize(keyword);
        String roleFilter = normalizeUpper(role);
        String statusFilter = normalizeUpper(status);

        var wrapper = Wrappers.<User>lambdaQuery()
            .and(
                kw != null,
                w ->
                    w
                        .like(User::getUsername, kw)
                        .or()
                        .like(User::getNickname, kw)
                        .or()
                        .like(User::getPhone, kw)
            )
            .eq(roleFilter != null, User::getRole, roleFilter)
            .eq(statusFilter != null, User::getStatus, statusFilter)
            .orderByDesc(User::getCreatedAt)
            .orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, UserRealName> realNames = loadRealNames(
            result.getRecords().stream().map(User::getId).toList()
        );
        List<AdminUserResponse> records = result
            .getRecords()
            .stream()
            .map(user -> toUserResponse(user, realNames.get(user.getId())))
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<AdminRealNameResponse> listRealNames(
        String status,
        String keyword,
        long pageNo,
        long pageSize
    ) {
        long safePage = Math.max(1, pageNo);
        long safeSize = Math.min(100, Math.max(1, pageSize));
        String statusFilter = normalizeUpper(status);
        String kw = normalize(keyword);

        var wrapper = Wrappers.<UserRealName>lambdaQuery()
            .eq(statusFilter != null, UserRealName::getStatus, statusFilter)
            .orderByDesc(UserRealName::getCreatedAt)
            .orderByDesc(UserRealName::getId);

        if (kw != null) {
            List<Long> matchedUserIds = searchUserIds(kw);
            if (matchedUserIds.isEmpty()) {
                wrapper.like(UserRealName::getRealName, kw);
            } else {
                wrapper.and(
                    w ->
                        w
                            .like(UserRealName::getRealName, kw)
                            .or()
                            .in(UserRealName::getUserId, matchedUserIds)
                );
            }
        }

        Page<UserRealName> result = userRealNameMapper.selectPage(
            new Page<>(safePage, safeSize),
            wrapper
        );
        Map<Long, User> users = loadUsers(
            result.getRecords().stream().map(UserRealName::getUserId).toList()
        );
        List<AdminRealNameResponse> records = result
            .getRecords()
            .stream()
            .map(realName -> toRealNameResponse(realName, users.get(realName.getUserId())))
            .toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public AdminUserResponse banUser(Long operatorId, Long userId) {
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能封禁自己的账号");
        }
        User user = requireUser(userId);
        if (PLATFORM_ADMIN.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能封禁平台管理员账号");
        }
        if (!BANNED.equals(user.getStatus())) {
            user.setStatus(BANNED);
            if (userMapper.updateById(user) == 0) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT);
            }
        }
        return toUserResponse(user, findRealName(userId));
    }

    @Transactional
    public AdminUserResponse unbanUser(Long userId) {
        User user = requireUser(userId);
        if (!ACTIVE.equals(user.getStatus())) {
            user.setStatus(ACTIVE);
            if (userMapper.updateById(user) == 0) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT);
            }
        }
        return toUserResponse(user, findRealName(userId));
    }

    @Transactional
    public AdminRealNameResponse approveRealName(Long id) {
        UserRealName realName = requirePendingRealName(id);
        User user = requireUser(realName.getUserId());

        realName.setStatus(VERIFIED);
        realName.setRejectReason(null);
        updateRealName(realName);

        if (!isAdmin(user.getRole())) {
            user.setRole(CERTIFIED);
            if (userMapper.updateById(user) == 0) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT);
            }
        }
        return toRealNameResponse(realName, user);
    }

    @Transactional
    public AdminRealNameResponse rejectRealName(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回理由不能为空");
        }
        UserRealName realName = requirePendingRealName(id);
        User user = requireUser(realName.getUserId());

        realName.setStatus(REJECTED);
        realName.setRejectReason(reason.trim());
        updateRealName(realName);
        return toRealNameResponse(realName, user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserRealName requirePendingRealName(Long id) {
        UserRealName realName = userRealNameMapper.selectById(id);
        if (realName == null) {
            throw new BusinessException(ErrorCode.REAL_NAME_NOT_FOUND);
        }
        if (!PENDING.equals(realName.getStatus())) {
            throw new BusinessException(
                ErrorCode.STATE_CONFLICT,
                "实名认证申请不在待审核状态"
            );
        }
        return realName;
    }

    private void updateRealName(UserRealName realName) {
        if (userRealNameMapper.updateById(realName) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
    }

    private UserRealName findRealName(Long userId) {
        return userRealNameMapper.selectOne(
            Wrappers.<UserRealName>lambdaQuery().eq(UserRealName::getUserId, userId)
        );
    }

    private List<Long> searchUserIds(String keyword) {
        return userMapper
            .selectList(
                Wrappers
                    .<User>lambdaQuery()
                    .select(User::getId)
                    .and(
                        w ->
                            w
                                .like(User::getUsername, keyword)
                                .or()
                                .like(User::getNickname, keyword)
                                .or()
                                .like(User::getPhone, keyword)
                    )
            )
            .stream()
            .map(User::getId)
            .toList();
    }

    private Map<Long, UserRealName> loadRealNames(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRealNameMapper
            .selectList(
                Wrappers
                    .<UserRealName>lambdaQuery()
                    .in(UserRealName::getUserId, userIds)
            )
            .stream()
            .collect(
                Collectors.toMap(
                    UserRealName::getUserId,
                    Function.identity(),
                    (left, right) -> right
                )
            );
    }

    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper
            .selectBatchIds(userIds)
            .stream()
            .collect(
                Collectors.toMap(User::getId, Function.identity(), (left, right) -> right)
            );
    }

    private AdminUserResponse toUserResponse(User user, UserRealName realName) {
        String realNameStatus = realName == null ? UNVERIFIED : realName.getStatus();
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getPhone(),
            user.getNickname(),
            user.getAvatarUrl(),
            user.getRole(),
            user.getStatus(),
            user.getCreditScore(),
            realNameStatus,
            realName == null ? null : realName.getRealName(),
            realName == null ? null : maskIdCard(realName.getIdCardNo()),
            realName == null ? null : realName.getRejectReason(),
            user.getCreatedAt()
        );
    }

    private AdminRealNameResponse toRealNameResponse(UserRealName realName, User user) {
        return new AdminRealNameResponse(
            realName.getId(),
            realName.getUserId(),
            user == null ? null : user.getUsername(),
            user == null ? null : user.getNickname(),
            realName.getRealName(),
            maskIdCard(realName.getIdCardNo()),
            realName.getStatus(),
            realName.getRejectReason(),
            realName.getCreatedAt(),
            realName.getUpdatedAt()
        );
    }

    private String maskIdCard(String storedIdCardNo) {
        return IdCardUtil.mask(idCardCipher.decrypt(storedIdCardNo));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private boolean isAdmin(String role) {
        return COMMUNITY_ADMIN.equals(role) || PLATFORM_ADMIN.equals(role);
    }
}
