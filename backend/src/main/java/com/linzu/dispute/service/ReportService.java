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
import com.linzu.platform.service.AuditLogService;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
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
    private final CommunityService communityService;
    private final AuditLogService auditLogService;

    // ===== UC-F7 提交举报 =====

    @Transactional
    public ReportResponse create(Long reporterId, ReportCreateRequest req) {
        String targetType = normalizeTarget(req.target());
        String reasonType = normalizeReason(req.type());

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
        if (!HANDLE_ACTIONS.contains(action)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知处理措施");
        }

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
