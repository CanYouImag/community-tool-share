package com.linzu.dispute.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.AuthUser;
import com.linzu.community.service.CommunityService;
import com.linzu.dispute.dto.ReportCreateRequest;
import com.linzu.dispute.dto.ReportHandleRequest;
import com.linzu.dispute.dto.ReportResponse;
import com.linzu.dispute.entity.Report;
import com.linzu.dispute.mapper.ReportMapper;
import com.linzu.item.entity.Item;
import com.linzu.item.enums.ItemStatus;
import com.linzu.item.mapper.ItemMapper;
import com.linzu.platform.service.AuditLogService;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import com.linzu.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String PENDING = "PENDING";
    private static final String PROCESSED = "PROCESSED";
    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private static final Set<String> HANDLE_ACTIONS = Set.of("REJECT", "ITEM_DOWN", "WARN", "BAN");

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final AdminUserService adminUserService;
    private final CommunityService communityService;
    private final AuditLogService auditLogService;

    // ===== UC-F7 提交举报 =====

    @Transactional
    public ReportResponse create(Long reporterId, ReportCreateRequest req) {
        String targetType = normalizeTarget(req.target());
        String reasonType = normalizeReason(req.type());
        validateTarget(targetType, req.targetId());

        if (req.targetId() != null) {
            Long dup = reportMapper.selectCount(
                    Wrappers.<Report>lambdaQuery()
                            .eq(Report::getTargetType, targetType)
                            .eq(Report::getTargetId, req.targetId())
                            .eq(Report::getStatus, PENDING)
            );
            if (dup != null && dup > 0) {
                throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
            }
        }

        Report r = new Report();
        r.setReporterId(reporterId);
        r.setTargetType(targetType);
        r.setTargetId(req.targetId());
        r.setCommunityId(req.communityId());
        r.setReasonType(reasonType);
        r.setDescription(req.desc().trim());
        r.setEvidenceUrls(joinUrls(req.evidenceUrls()));
        r.setStatus(PENDING);
        reportMapper.insert(r);
        return toResponse(r);
    }

    // ===== 我的举报 =====

    public List<ReportResponse> listMine(Long reporterId) {
        List<Report> list = reportMapper.selectList(
                Wrappers.<Report>lambdaQuery()
                        .eq(Report::getReporterId, reporterId)
                        .orderByDesc(Report::getId)
        );
        return list.stream().map(this::toResponse).toList();
    }

    // ===== UC-F8 处理举报 =====

    public PageResult<ReportResponse> listForAdmin(Long operatorId, String status, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));

        var wrapper = Wrappers.<Report>lambdaQuery()
                .eq(StringUtils.hasText(status), Report::getStatus, status)
                .orderByAsc(Report::getId);

        // BR-13：社区管理员仅可处理本社区举报；系统管理员处理全局
        if (!PLATFORM_ADMIN.equals(AuthContext.require().role())) {
            long adminCommunityId = communityService.requireAdminCommunityId(operatorId);
            wrapper.eq(Report::getCommunityId, adminCommunityId);
        }

        Page<Report> p = new Page<>(safePage, safeSize);
        Page<Report> result = reportMapper.selectPage(p, wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize()
        );
    }

    @Transactional
    public ReportResponse handle(Long operatorId, Long reportId, ReportHandleRequest req) {
        Report r = requireReport(reportId);
        if (!PENDING.equals(r.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }

        // BR-13：社区管理员仅限本社区举报
        AuthUser auth = AuthContext.require();
        if (!PLATFORM_ADMIN.equals(auth.role())) {
            long adminCommunityId = communityService.requireAdminCommunityId(operatorId);
            if (r.getCommunityId() == null || !r.getCommunityId().equals(adminCommunityId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        String action = req.action().trim();
        // 处理动作需要真正联动到物品/用户，而不是只更新举报单。
        if (!HANDLE_ACTIONS.contains(action)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知处理措施");
        }

        applyAction(operatorId, auth, r, action, req.note());

        r.setHandleAction(action);
        r.setHandleNote(req.note());
        r.setHandledBy(operatorId);
        r.setHandledAt(LocalDateTime.now());
        r.setStatus(PROCESSED);
        reportMapper.updateById(r);
        auditLogService.record(operatorId, "HANDLE_REPORT", "REPORT", reportId,
                "处理举报#" + reportId + "：" + action);
        // TODO: 处置联动 UC-C7（下架物品）/ UC-G3（封禁）待对应模块就绪后接入
        return toResponse(r);
    }

    // ===== 内部工具 =====

    private void applyAction(
            Long operatorId,
            AuthUser auth,
            Report report,
            String action,
            String note
    ) {
        String reason = StringUtils.hasText(note) ? note.trim() : "举报处理";
        switch (action) {
            case "ITEM_DOWN" -> {
                if (!"ITEM".equals(report.getTargetType())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "只有物品举报可以下架物品");
                }
                takeDownItem(report.getTargetId(), reason);
            }
            case "BAN" -> {
                if (!PLATFORM_ADMIN.equals(auth.role())) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "只有平台管理员可以封禁账号");
                }
                if ("USER".equals(report.getTargetType())) {
                    adminUserService.banUser(operatorId, report.getTargetId());
                } else if ("ITEM".equals(report.getTargetType())) {
                    Item item = takeDownItem(report.getTargetId(), reason);
                    adminUserService.banUser(operatorId, item.getOwnerId());
                } else {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "该举报目标不支持封禁");
                }
            }
            case "WARN", "REJECT" -> {
                // 仅记录处理结果，不改业务状态。
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知处理措施");
        }
    }

    private Item takeDownItem(Long itemId, String reason) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!ItemStatus.VIOLATION_OFF.name().equals(item.getStatus())) {
            item.setStatus(ItemStatus.VIOLATION_OFF.name());
            item.setViolationReason("举报处理：" + reason);
            item.setIsPinned(0);
            itemMapper.updateById(item);
        }
        return item;
    }

    private void validateTarget(String targetType, Long targetId) {
        if (targetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "举报目标不能为空");
        }
        if ("ITEM".equals(targetType) && itemMapper.selectById(targetId) == null) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if ("USER".equals(targetType) && userMapper.selectById(targetId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Report requireReport(Long reportId) {
        Report r = reportMapper.selectById(reportId);
        if (r == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return r;
    }

    private ReportResponse toResponse(Report r) {
        return new ReportResponse(
                r.getId(),
                r.getReporterId(),
                userName(r.getReporterId()),
                r.getCommunityId(),
                r.getTargetType(),
                r.getTargetId(),
                r.getReasonType(),
                r.getDescription(),
                splitUrls(r.getEvidenceUrls()),
                r.getStatus(),
                r.getHandleAction(),
                r.getHandleNote(),
                r.getHandledBy(),
                userName(r.getHandledBy()),
                r.getHandledAt(),
                r.getCreatedAt()
        );
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }

    /** 举报对象：兼容前端中文标签与后端码值。 */
    private String normalizeTarget(String v) {
        if (v == null) {
            return null;
        }
        return switch (v.trim()) {
            case "物品", "ITEM" -> "ITEM";
            case "用户", "USER" -> "USER";
            case "评价", "REVIEW" -> "REVIEW";
            default -> v.trim();
        };
    }

    /** 举报类型：兼容前端中文标签与后端码值。 */
    private String normalizeReason(String v) {
        if (v == null) {
            return null;
        }
        return switch (v.trim()) {
            case "信息不实", "FALSE_INFO" -> "FALSE_INFO";
            case "违规内容", "ILLEGAL_CONTENT" -> "ILLEGAL_CONTENT";
            case "恶意评价", "MALICIOUS_REVIEW" -> "MALICIOUS_REVIEW";
            case "其他", "OTHER" -> "OTHER";
            default -> v.trim();
        };
    }

    private String joinUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        return String.join(",", urls.stream().filter(StringUtils::hasText).map(String::trim).toList());
    }

    private List<String> splitUrls(String urls) {
        if (!StringUtils.hasText(urls)) {
            return List.of();
        }
        return Arrays.stream(urls.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }
}
