package com.linzu.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.community.entity.Community;
import com.linzu.community.entity.CommunityMember;
import com.linzu.community.mapper.CommunityMapper;
import com.linzu.community.mapper.CommunityMemberMapper;
import com.linzu.community.service.CommunityService;
import com.linzu.dispute.entity.Dispute;
import com.linzu.dispute.mapper.DisputeMapper;
import com.linzu.platform.dto.CommunityAdminResponse;
import com.linzu.platform.dto.CommunityCreateRequest;
import com.linzu.platform.dto.CommunityUpdateRequest;
import com.linzu.platform.dto.ConfigUpdateRequest;
import com.linzu.platform.dto.UserAdminResponse;
import com.linzu.platform.entity.PlatformConfig;
import com.linzu.order.entity.Order;
import com.linzu.order.mapper.OrderMapper;
import com.linzu.platform.mapper.PlatformConfigMapper;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformService {

    private static final String CERTIFIED = "CERTIFIED";
    private static final String COMMUNITY_ADMIN = "COMMUNITY_ADMIN";
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String BANNED = "BANNED";
    private static final String ACTIVE = "ACTIVE";
    private static final String ADMIN = "ADMIN";
    private static final String MEMBER = "MEMBER";

    private static final String KEY_COMMISSION_RATE = "commission_rate";
    private static final String KEY_COMMUNITY_AUTO_APPROVE = "community_auto_approve";
    private static final String KEY_ITEM_AUDIT_ENABLED = "item_audit_enabled";

    private final CommunityMapper communityMapper;
    private final CommunityMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final DisputeMapper disputeMapper;
    private final OrderMapper orderMapper;
    private final PlatformConfigMapper configMapper;
    private final CommunityService communityService;
    private final AuditLogService auditLogService;

    // ===== UC-G1 社区 CRUD =====

    public List<CommunityAdminResponse> listCommunities(String keyword) {
        List<Community> list = communityMapper.selectList(
                Wrappers.<Community>lambdaQuery()
                        .like(StringUtils.hasText(keyword), Community::getName, keyword)
                        .orderByAsc(Community::getId)
        );
        return list.stream().map(this::toAdminResponse).toList();
    }

    @Transactional
    public CommunityAdminResponse createCommunity(Long operatorId, CommunityCreateRequest req) {
        ensureNameUnique(req.name().trim(), null);
        Community c = new Community();
        c.setName(req.name().trim());
        c.setIntro(req.intro());
        c.setCoverUrl(req.coverUrl());
        c.setAutoApprove(Boolean.TRUE.equals(req.autoApprove()));
        c.setMemberCount(0);
        c.setItemCount(0);
        c.setStatus(ACTIVE);
        communityMapper.insert(c);
        auditLogService.record(operatorId, "CREATE_COMMUNITY", "COMMUNITY", c.getId(), "创建社区：" + c.getName());
        return toAdminResponse(c);
    }

    @Transactional
    public CommunityAdminResponse updateCommunity(Long operatorId, Long communityId, CommunityUpdateRequest req) {
        Community c = communityMapper.selectById(communityId);
        if (c == null) {
            throw new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND);
        }
        ensureNameUnique(req.name().trim(), communityId);
        c.setName(req.name().trim());
        c.setIntro(req.intro());
        c.setCoverUrl(req.coverUrl());
        if (req.autoApprove() != null) {
            c.setAutoApprove(req.autoApprove());
        }
        if (StringUtils.hasText(req.status())) {
            c.setStatus(req.status().trim());
        }
        communityMapper.updateById(c);
        auditLogService.record(operatorId, "UPDATE_COMMUNITY", "COMMUNITY", communityId, "编辑社区：" + c.getName());
        return toAdminResponse(c);
    }

    // ===== UC-G2 任命/罢免社区管理员 =====

    @Transactional
    public CommunityAdminResponse appointAdmin(Long operatorId, Long communityId, Long userId) {
        Community c = requireCommunity(communityId);
        User user = requireUser(userId);
        // 校验顺序：先“本社区是否已有管理员”，再“该用户是否已在别处任职”，
        // 最后才校验实名认证，避免对已任社区管理员的用户误报“未通过实名认证”。
        Long hasAdmin = memberMapper.selectCount(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getRole, ADMIN)
                        .eq(CommunityMember::getStatus, ACTIVE)
        );
        if (hasAdmin != null && hasAdmin > 0) {
            throw new BusinessException(ErrorCode.COMMUNITY_ADMIN_EXISTS);
        }
        Long otherAdmin = memberMapper.selectCount(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getUserId, userId)
                        .eq(CommunityMember::getRole, ADMIN)
                        .eq(CommunityMember::getStatus, ACTIVE)
                        .ne(CommunityMember::getCommunityId, communityId)
        );
        if (otherAdmin != null && otherAdmin > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ADMIN);
        }
        if (!CERTIFIED.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_CERTIFIED);
        }

        CommunityMember m = memberMapper.selectOne(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getUserId, userId)
        );
        if (m == null) {
            m = new CommunityMember();
            m.setCommunityId(communityId);
            m.setUserId(userId);
            m.setRole(ADMIN);
            m.setStatus(ACTIVE);
            memberMapper.insert(m);
            c.setMemberCount((c.getMemberCount() == null ? 0 : c.getMemberCount()) + 1);
            communityMapper.updateById(c);
        } else {
            m.setRole(ADMIN);
            m.setStatus(ACTIVE);
            memberMapper.updateById(m);
        }

        user.setRole(COMMUNITY_ADMIN);
        userMapper.updateById(user);
        auditLogService.record(operatorId, "APPOINT_ADMIN", "COMMUNITY", communityId,
                "任命 " + user.getNickname() + " 为社区管理员");
        return toAdminResponse(c);
    }

    @Transactional
    public CommunityAdminResponse dismissAdmin(Long operatorId, Long communityId, Long userId) {
        Community c = requireCommunity(communityId);
        CommunityMember m = memberMapper.selectOne(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getUserId, userId)
                        .eq(CommunityMember::getRole, ADMIN)
                        .eq(CommunityMember::getStatus, ACTIVE)
        );
        if (m == null) {
            throw new BusinessException(ErrorCode.NOT_COMMUNITY_ADMIN);
        }
        User user = userMapper.selectById(userId);
        if (user != null && PLATFORM_ADMIN.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统管理员不可罢免");
        }
        m.setRole(MEMBER);
        memberMapper.updateById(m);

        if (user != null && COMMUNITY_ADMIN.equals(user.getRole())) {
            user.setRole(CERTIFIED);
            userMapper.updateById(user);
        }
        auditLogService.record(operatorId, "DISMISS_ADMIN", "COMMUNITY", communityId,
                "罢免社区管理员 " + (user == null ? userId : user.getNickname()));
        return toAdminResponse(c);
    }

    // ===== UC-G3 封禁/解封账号 =====

    public PageResult<UserAdminResponse> listUsers(String keyword, String status, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));

        var wrapper = Wrappers.<User>lambdaQuery()
                .eq(StringUtils.hasText(status), User::getStatus, status)
                .orderByAsc(User::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }

        Page<User> p = new Page<>(safePage, safeSize);
        Page<User> result = userMapper.selectPage(p, wrapper);
        List<UserAdminResponse> records = result.getRecords().stream().map(u -> new UserAdminResponse(
                u.getId(), u.getUsername(), u.getNickname(), u.getAvatarUrl(),
                u.getRole(), u.getStatus(), u.getCreditScore(), u.getCreatedAt()
        )).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void banUser(Long operatorId, Long userId, String reason) {
        User user = requireUser(userId);
        user.setStatus(BANNED);
        userMapper.updateById(user);
        auditLogService.record(operatorId, "BAN_USER", "USER", userId, reason);
        // TODO: BR-22 在途订单转纠纷、登录会话失效（订单域/认证域就绪后接入）
    }

    @Transactional
    public void unbanUser(Long operatorId, Long userId, String reason) {
        User user = requireUser(userId);
        user.setStatus(ACTIVE);
        userMapper.updateById(user);
        auditLogService.record(operatorId, "UNBAN_USER", "USER", userId, reason);
    }

    // ===== UC-G4 平台参数配置 =====

    public Map<String, Object> getConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("commissionRate", new BigDecimal(getValue(KEY_COMMISSION_RATE, "0.10")));
        result.put("communityAutoApprove", Boolean.parseBoolean(getValue(KEY_COMMUNITY_AUTO_APPROVE, "false")));
        result.put("itemAuditEnabled", Boolean.parseBoolean(getValue(KEY_ITEM_AUDIT_ENABLED, "true")));
        return result;
    }

    @Transactional
    public Map<String, Object> updateConfig(Long operatorId, ConfigUpdateRequest req) {
        if (req.commissionRate() != null) {
            if (req.commissionRate().compareTo(BigDecimal.ZERO) < 0
                    || req.commissionRate().compareTo(new BigDecimal("0.50")) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "佣金比例需在 0~50% 之间");
            }
            upsertConfig(KEY_COMMISSION_RATE, req.commissionRate().setScale(4, RoundingMode.HALF_UP).toPlainString());
        }
        if (req.communityAutoApprove() != null) {
            upsertConfig(KEY_COMMUNITY_AUTO_APPROVE, String.valueOf(req.communityAutoApprove()));
        }
        if (req.itemAuditEnabled() != null) {
            upsertConfig(KEY_ITEM_AUDIT_ENABLED, String.valueOf(req.itemAuditEnabled()));
        }
        auditLogService.record(operatorId, "UPDATE_CONFIG", "PLATFORM", null, "更新平台参数");
        return getConfig();
    }

    // ===== UC-G5 / UC-G7 数据统计 =====

    public Map<String, Object> platformStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userMapper.selectCount(null));
        result.put("communityCount", communityMapper.selectCount(null));
        result.put("disputeCount", disputeMapper.selectCount(null));
        result.put("orderCount", orderMapper.selectCount(null));
        result.put("gmv", completedRent(null));
        return result;
    }

    public Map<String, Object> communityStats(Long operatorId) {
        long communityId = communityService.requireAdminCommunityId(operatorId);
        Community c = requireCommunity(communityId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("communityId", communityId);
        result.put("memberCount", memberMapper.selectCount(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getStatus, ACTIVE)
        ));
        result.put("itemCount", c.getItemCount() == null ? 0 : c.getItemCount());
        result.put("disputeCount", disputeMapper.selectCount(
                Wrappers.<Dispute>lambdaQuery().eq(Dispute::getCommunityId, communityId)
        ));
        result.put("orderCount", orderMapper.selectCount(
                Wrappers.<Order>lambdaQuery().eq(Order::getCommunityId, communityId)
        ));
        result.put("gmv", completedRent(communityId));
        return result;
    }

    // ===== 内部工具 =====

    private BigDecimal completedRent(Long communityId) {
        var wrapper = Wrappers
            .<Order>query()
            .select("COALESCE(SUM(rent_amount), 0) AS gmv")
            .eq("status", "COMPLETED");
        if (communityId != null) {
            wrapper.eq("community_id", communityId);
        }
        List<Map<String, Object>> rows = orderMapper.selectMaps(wrapper);
        if (rows.isEmpty() || rows.get(0).get("gmv") == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(rows.get(0).get("gmv").toString())
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Community requireCommunity(Long communityId) {
        Community c = communityMapper.selectById(communityId);
        if (c == null) {
            throw new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND);
        }
        return c;
    }

    private User requireUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return u;
    }

    private void ensureNameUnique(String name, Long excludeId) {
        Long count = communityMapper.selectCount(
                Wrappers.<Community>lambdaQuery()
                        .eq(Community::getName, name)
                        .ne(excludeId != null, Community::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.COMMUNITY_NAME_EXISTS);
        }
    }

    private CommunityAdminResponse toAdminResponse(Community c) {
        CommunityMember admin = findAdmin(c.getId());
        Long adminId = admin == null ? null : admin.getUserId();
        String adminName = admin == null ? null : userName(admin.getUserId());
        String adminRole = admin == null ? null : userRole(admin.getUserId());
        return new CommunityAdminResponse(
                c.getId(),
                c.getName(),
                c.getIntro(),
                c.getCoverUrl(),
                Boolean.TRUE.equals(c.getAutoApprove()),
                c.getMemberCount(),
                c.getItemCount(),
                c.getStatus(),
                adminId,
                adminName,
                adminRole,
                c.getCreatedAt()
        );
    }

    private CommunityMember findAdmin(Long communityId) {
        return memberMapper.selectOne(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getRole, ADMIN)
                        .eq(CommunityMember::getStatus, ACTIVE)
                        .last("LIMIT 1")
        );
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }

    private String userRole(Long userId) {
        if (userId == null) {
            return null;
        }
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getRole();
    }

    private String getValue(String key, String defaultValue) {
        PlatformConfig config = configMapper.selectOne(
                Wrappers.<PlatformConfig>lambdaQuery().eq(PlatformConfig::getConfigKey, key)
        );
        return config == null ? defaultValue : config.getConfigValue();
    }

    private void upsertConfig(String key, String value) {
        PlatformConfig config = configMapper.selectOne(
                Wrappers.<PlatformConfig>lambdaQuery().eq(PlatformConfig::getConfigKey, key)
        );
        if (config == null) {
            config = new PlatformConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription("平台参数");
            configMapper.insert(config);
        } else {
            config.setConfigValue(value);
            configMapper.updateById(config);
        }
    }
}
