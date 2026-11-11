package com.linzu.community.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.AuthUser;
import com.linzu.community.dto.AnnouncementRequest;
import com.linzu.community.dto.AnnouncementResponse;
import com.linzu.community.dto.AuditJoinApplyRequest;
import com.linzu.community.dto.CommunityMemberResponse;
import com.linzu.community.dto.CommunityResponse;
import com.linzu.community.dto.JoinApplyResponse;
import com.linzu.community.dto.MembershipResponse;
import com.linzu.community.entity.Community;
import com.linzu.community.entity.CommunityAnnouncement;
import com.linzu.community.entity.CommunityJoinApply;
import com.linzu.community.entity.CommunityMember;
import com.linzu.community.mapper.CommunityAnnouncementMapper;
import com.linzu.community.mapper.CommunityJoinApplyMapper;
import com.linzu.community.mapper.CommunityMapper;
import com.linzu.community.mapper.CommunityMemberMapper;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String MEMBER = "MEMBER";
    private static final String ADMIN = "ADMIN";
    private static final String ACTIVE = "ACTIVE";
    private static final String LEFT = "LEFT";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String JOINED = "JOINED";

    private final CommunityMapper communityMapper;
    private final CommunityMemberMapper memberMapper;
    private final CommunityAnnouncementMapper announcementMapper;
    private final CommunityJoinApplyMapper joinApplyMapper;
    private final UserMapper userMapper;

    // ===== UC-B1 浏览社区 =====

    public List<CommunityResponse> list(String keyword) {
        List<Community> list = communityMapper.selectList(
                Wrappers.<Community>lambdaQuery()
                        .eq(Community::getStatus, ACTIVE)
                        .like(StringUtils.hasText(keyword), Community::getName, keyword)
                        .orderByAsc(Community::getId)
        );
        return list.stream().map(this::toResponse).toList();
    }

    public Map<String, Object> home(Long communityId) {
        Community community = requireCommunity(communityId);
        CommunityResponse base = toResponse(community);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", base.id());
        result.put("name", base.name());
        result.put("intro", base.intro());
        result.put("coverUrl", base.coverUrl());
        result.put("autoApprove", base.autoApprove());
        result.put("members", base.members());
        result.put("onRent", base.onRent());
        result.put("admin", base.admin());
        result.put("notice", base.notice());
        result.put("announcements", listAnnouncements(communityId));
        // 物品域（窦源泽）就绪后填充置顶/最新上架物品
        result.put("top", List.of());
        result.put("latest", List.of());
        return result;
    }

    // ===== UC-B2 申请加入 =====

    @Transactional
    public Map<String, Object> join(Long userId, Long communityId) {
        Community community = requireCommunity(communityId);
        if (findMember(communityId, userId) != null) {
            throw new BusinessException(ErrorCode.ALREADY_IN_COMMUNITY);
        }
        Long pending = joinApplyMapper.selectCount(
                Wrappers.<CommunityJoinApply>lambdaQuery()
                        .eq(CommunityJoinApply::getCommunityId, communityId)
                        .eq(CommunityJoinApply::getUserId, userId)
                        .eq(CommunityJoinApply::getStatus, PENDING)
        );
        if (pending != null && pending > 0) {
            throw new BusinessException(ErrorCode.JOIN_APPLY_EXISTS);
        }

        String status;
        if (Boolean.TRUE.equals(community.getAutoApprove())) {
            addMember(community, userId, MEMBER);
            status = JOINED;
        } else {
            CommunityJoinApply apply = new CommunityJoinApply();
            apply.setCommunityId(communityId);
            apply.setUserId(userId);
            apply.setStatus(PENDING);
            joinApplyMapper.insert(apply);
            status = PENDING;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("communityId", communityId);
        result.put("status", status);
        return result;
    }

    // ===== UC-B3 审核加入申请 =====

    public PageResult<JoinApplyResponse> listJoinApplies(
            Long operatorId, Long communityId, long page, long size, String status
    ) {
        long adminCommunityId = resolveCommunityId(operatorId, communityId);
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));

        Page<CommunityJoinApply> p = new Page<>(safePage, safeSize);
        Page<CommunityJoinApply> result = joinApplyMapper.selectPage(p,
                Wrappers.<CommunityJoinApply>lambdaQuery()
                        .eq(CommunityJoinApply::getCommunityId, adminCommunityId)
                        .eq(StringUtils.hasText(status), CommunityJoinApply::getStatus, status)
                        .orderByDesc(CommunityJoinApply::getCreatedAt)
        );
        List<JoinApplyResponse> records = result.getRecords().stream().map(a -> {
            User u = userMapper.selectById(a.getUserId());
            return new JoinApplyResponse(
                    a.getId(),
                    a.getUserId(),
                    u == null ? null : u.getNickname(),
                    u == null ? null : u.getAvatarUrl(),
                    a.getApplyReason(),
                    a.getStatus(),
                    a.getCreatedAt()
            );
        }).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void auditJoin(Long operatorId, Long communityId, Long applyId, AuditJoinApplyRequest request) {
        long adminCommunityId = resolveCommunityId(operatorId, communityId);
        Community community = requireCommunity(adminCommunityId);
        CommunityJoinApply apply = joinApplyMapper.selectById(applyId);
        if (apply == null || !apply.getCommunityId().equals(adminCommunityId)) {
            throw new BusinessException(ErrorCode.JOIN_APPLY_NOT_FOUND);
        }
        if (!PENDING.equals(apply.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        apply.setHandledBy(operatorId);
        apply.setHandledAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(request.approve())) {
            apply.setStatus(APPROVED);
            joinApplyMapper.updateById(apply);
            addMember(community, apply.getUserId(), MEMBER);
        } else {
            apply.setStatus(REJECTED);
            joinApplyMapper.updateById(apply);
        }
    }

    // ===== UC-B4 退出社区 =====

    @Transactional
    public void quit(Long userId, Long communityId) {
        Community community = requireCommunity(communityId);
        CommunityMember member = findMember(communityId, userId);
        if (member == null) {
            throw new BusinessException(ErrorCode.COMMUNITY_MEMBER_NOT_FOUND);
        }
        if (ADMIN.equals(member.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "社区管理员不能退出社区");
        }
        // TODO: BR-17 有进行中订单时禁止退出，订单域（窦源泽）就绪后接入
        member.setStatus(LEFT);
        memberMapper.updateById(member);
        decrementMemberCount(community);
    }

    // ===== UC-B5 社区公告 =====

    public List<AnnouncementResponse> listAnnouncements(Long communityId) {
        requireCommunity(communityId);
        List<CommunityAnnouncement> list = announcementMapper.selectList(
                Wrappers.<CommunityAnnouncement>lambdaQuery()
                        .eq(CommunityAnnouncement::getCommunityId, communityId)
                        .orderByDesc(CommunityAnnouncement::getId)
        );
        return list.stream().map(this::toAnnouncementResponse).toList();
    }

    @Transactional
    public AnnouncementResponse publishAnnouncement(Long operatorId, Long communityId, AnnouncementRequest request) {
        long adminCommunityId = resolveCommunityId(operatorId, communityId);
        requireCommunity(adminCommunityId);
        CommunityAnnouncement a = new CommunityAnnouncement();
        a.setCommunityId(adminCommunityId);
        a.setTitle(request.title().trim());
        a.setContent(request.content().trim());
        a.setImageUrl(request.imageUrl());
        a.setCreatedBy(operatorId);
        announcementMapper.insert(a);
        return toAnnouncementResponse(a);
    }

    @Transactional
    public void deleteAnnouncement(Long operatorId, Long communityId, Long announcementId) {
        long adminCommunityId = resolveCommunityId(operatorId, communityId);
        CommunityAnnouncement a = announcementMapper.selectById(announcementId);
        if (a == null || !a.getCommunityId().equals(adminCommunityId)) {
            throw new BusinessException(ErrorCode.COMMUNITY_ANNOUNCEMENT_NOT_FOUND);
        }
        announcementMapper.deleteById(announcementId);
    }

    // ===== UC-B6 成员管理 =====

    public PageResult<CommunityMemberResponse> listMembers(
            Long operatorId, Long communityId, long page, long size
    ) {
        long adminCommunityId = resolveCommunityId(operatorId, communityId);
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));

        Page<CommunityMember> p = new Page<>(safePage, safeSize);
        Page<CommunityMember> result = memberMapper.selectPage(p,
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, adminCommunityId)
                        .eq(CommunityMember::getStatus, ACTIVE)
                        .orderByAsc(CommunityMember::getCreatedAt)
        );
        List<CommunityMemberResponse> records = result.getRecords().stream().map(m -> {
            User u = userMapper.selectById(m.getUserId());
            return new CommunityMemberResponse(
                    m.getUserId(),
                    u == null ? null : u.getNickname(),
                    u == null ? null : u.getAvatarUrl(),
                    m.getRole(),
                    m.getCreatedAt()
            );
        }).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void kick(Long operatorId, Long communityId, Long targetUserId) {
        long adminCommunityId = resolveCommunityId(operatorId, communityId);
        Community community = requireCommunity(adminCommunityId);
        CommunityMember target = findMember(adminCommunityId, targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.COMMUNITY_MEMBER_NOT_FOUND);
        }
        if (ADMIN.equals(target.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能踢出社区管理员");
        }
        // TODO: BR-17 有进行中订单时禁止踢出，订单域（窦源泽）就绪后接入
        target.setStatus(LEFT);
        memberMapper.updateById(target);
        decrementMemberCount(community);
    }

    // ===== 我的社区成员关系（供前端 memberships/mine 使用）=====

    public List<MembershipResponse> myMemberships(Long userId) {
        List<CommunityMember> members = memberMapper.selectList(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getUserId, userId)
                        .eq(CommunityMember::getStatus, ACTIVE)
        );
        List<CommunityJoinApply> applies = joinApplyMapper.selectList(
                Wrappers.<CommunityJoinApply>lambdaQuery()
                        .eq(CommunityJoinApply::getUserId, userId)
                        .eq(CommunityJoinApply::getStatus, PENDING)
        );
        List<MembershipResponse> result = new ArrayList<>();
        for (CommunityMember m : members) {
            Community c = communityMapper.selectById(m.getCommunityId());
            if (c != null) {
                result.add(new MembershipResponse(c.getId(), c.getName(), JOINED, m.getCreatedAt().format(DATE_FMT)));
            }
        }
        for (CommunityJoinApply a : applies) {
            Community c = communityMapper.selectById(a.getCommunityId());
            if (c != null) {
                result.add(new MembershipResponse(c.getId(), c.getName(), PENDING, a.getCreatedAt().format(DATE_FMT)));
            }
        }
        return result;
    }

    // ===== 内部工具 =====

    public Community requireCommunity(Long communityId) {
        Community c = communityMapper.selectById(communityId);
        if (c == null) {
            throw new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND);
        }
        return c;
    }

    /**
     * 解析“可操作社区”。系统管理员可操作任意指定社区；社区管理员严格限定在本社区（BR-13 行级隔离）。
     */
    private long resolveCommunityId(Long operatorId, Long pathCommunityId) {
        AuthUser auth = AuthContext.require();
        if ("PLATFORM_ADMIN".equals(auth.role())) {
            if (pathCommunityId == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少社区 ID");
            }
            return pathCommunityId;
        }
        long adminCommunityId = requireAdminCommunityId(operatorId);
        if (pathCommunityId != null && pathCommunityId != adminCommunityId) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return adminCommunityId;
    }

    private long requireAdminCommunityId(Long operatorId) {
        CommunityMember admin = memberMapper.selectOne(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getUserId, operatorId)
                        .eq(CommunityMember::getRole, ADMIN)
                        .eq(CommunityMember::getStatus, ACTIVE)
                        .last("LIMIT 1")
        );
        if (admin == null) {
            throw new BusinessException(ErrorCode.NOT_COMMUNITY_ADMIN);
        }
        return admin.getCommunityId();
    }

    private CommunityMember findMember(Long communityId, Long userId) {
        return memberMapper.selectOne(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getUserId, userId)
                        .eq(CommunityMember::getStatus, ACTIVE)
        );
    }

    private void addMember(Community community, Long userId, String role) {
        CommunityMember member = new CommunityMember();
        member.setCommunityId(community.getId());
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(ACTIVE);
        memberMapper.insert(member);
        community.setMemberCount((community.getMemberCount() == null ? 0 : community.getMemberCount()) + 1);
        communityMapper.updateById(community);
    }

    private void decrementMemberCount(Community community) {
        int count = community.getMemberCount() == null ? 0 : community.getMemberCount();
        community.setMemberCount(Math.max(0, count - 1));
        communityMapper.updateById(community);
    }

    private CommunityResponse toResponse(Community c) {
        return new CommunityResponse(
                c.getId(),
                c.getName(),
                c.getIntro(),
                c.getCoverUrl(),
                Boolean.TRUE.equals(c.getAutoApprove()),
                c.getMemberCount() == null ? 0 : c.getMemberCount(),
                c.getItemCount() == null ? 0 : c.getItemCount(),
                findAdminName(c.getId()),
                findLatestNotice(c.getId())
        );
    }

    private String findAdminName(Long communityId) {
        CommunityMember admin = memberMapper.selectOne(
                Wrappers.<CommunityMember>lambdaQuery()
                        .eq(CommunityMember::getCommunityId, communityId)
                        .eq(CommunityMember::getRole, ADMIN)
                        .eq(CommunityMember::getStatus, ACTIVE)
                        .last("LIMIT 1")
        );
        if (admin == null) {
            return null;
        }
        User user = userMapper.selectById(admin.getUserId());
        return user == null ? null : user.getNickname();
    }

    private String findLatestNotice(Long communityId) {
        List<CommunityAnnouncement> list = announcementMapper.selectList(
                Wrappers.<CommunityAnnouncement>lambdaQuery()
                        .eq(CommunityAnnouncement::getCommunityId, communityId)
                        .orderByDesc(CommunityAnnouncement::getId)
                        .last("LIMIT 1")
        );
        return list.isEmpty() ? null : list.get(0).getContent();
    }

    private AnnouncementResponse toAnnouncementResponse(CommunityAnnouncement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getContent(), a.getImageUrl(), a.getCreatedAt());
    }
}
