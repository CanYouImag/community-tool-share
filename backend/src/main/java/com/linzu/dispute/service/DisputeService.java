package com.linzu.dispute.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.common.exception.BusinessException;
import com.linzu.common.exception.ErrorCode;
import com.linzu.common.security.AuthContext;
import com.linzu.common.security.AuthUser;
import com.linzu.community.entity.Community;
import com.linzu.community.entity.CommunityMember;
import com.linzu.community.mapper.CommunityMapper;
import com.linzu.community.mapper.CommunityMemberMapper;
import com.linzu.community.service.CommunityService;
import com.linzu.dispute.dto.ArbitrateRequest;
import com.linzu.dispute.dto.AwardResponse;
import com.linzu.dispute.dto.DisputeCreateRequest;
import com.linzu.dispute.dto.DisputeDetailResponse;
import com.linzu.dispute.dto.DisputeEvidenceResponse;
import com.linzu.dispute.dto.DisputeMessageResponse;
import com.linzu.dispute.dto.DisputeResponse;
import com.linzu.dispute.dto.EscalateRequest;
import com.linzu.dispute.dto.MediateRequest;
import com.linzu.dispute.dto.RenterRespondRequest;
import com.linzu.dispute.dto.ResolveRequest;
import com.linzu.dispute.entity.Dispute;
import com.linzu.dispute.entity.DisputeArbitration;
import com.linzu.dispute.entity.DisputeEvidence;
import com.linzu.dispute.entity.DisputeMessage;
import com.linzu.dispute.mapper.DisputeArbitrationMapper;
import com.linzu.dispute.mapper.DisputeEvidenceMapper;
import com.linzu.dispute.mapper.DisputeMapper;
import com.linzu.dispute.mapper.DisputeMessageMapper;
import com.linzu.platform.service.AuditLogService;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import com.linzu.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final String AWAITING_RENTER = "AWAITING_RENTER";
    private static final String MEDIATING = "MEDIATING";
    private static final String PENDING_ARBITRATION = "PENDING_ARBITRATION";
    private static final String RESOLVED = "RESOLVED";
    private static final String ARBITRATED = "ARBITRATED";
    private static final String SETTLED = "SETTLED";

    private static final String REFUND_ALL = "REFUND_ALL";
    private static final String PARTIAL_DEDUCT = "PARTIAL_DEDUCT";
    private static final String DEDUCT_ALL = "DEDUCT_ALL";

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private static final String COMMUNITY_ADMIN = "COMMUNITY_ADMIN";

    private final DisputeMapper disputeMapper;
    private final DisputeEvidenceMapper evidenceMapper;
    private final DisputeMessageMapper messageMapper;
    private final DisputeArbitrationMapper arbitrationMapper;
    private final UserMapper userMapper;
    private final CommunityMapper communityMapper;
    private final CommunityMemberMapper communityMemberMapper;
    private final CommunityService communityService;
    private final WalletService walletService;
    private final AuditLogService auditLogService;

    // ===== UC-F1 发起纠纷 =====

    @Transactional
    public DisputeResponse create(Long initiatorId, DisputeCreateRequest req) {
        // 仅出租者（物主）可提出扣留押金
        if (!req.ownerId().equals(initiatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 一个订单一生只能发起一次纠纷：无论进行中（待回应/调解中/待仲裁）还是已终结（和解/调解/裁决），均不可重复发起
        Long existing = disputeMapper.selectCount(
                Wrappers.<Dispute>lambdaQuery()
                        .eq(Dispute::getOrderId, req.orderId())
        );
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.ALREADY_DISPUTED);
        }

        BigDecimal deposit = money(req.depositAmount());
        BigDecimal withhold = money(req.withholdAmount());
        if (withhold.compareTo(BigDecimal.ZERO) <= 0 || withhold.compareTo(deposit) > 0) {
            throw new BusinessException(ErrorCode.DEDUCT_EXCEEDS_DEPOSIT);
        }

        Dispute d = new Dispute();
        d.setOrderId(req.orderId());
        d.setCommunityId(req.communityId());
        d.setInitiatorId(initiatorId);
        d.setRenterId(req.renterId());
        d.setOwnerId(req.ownerId());
        d.setReason(req.reason().trim());
        d.setDepositAmount(deposit);
        d.setRentAmount(money(req.rentAmount()));
        d.setWithholdAmount(withhold);
        d.setStatus(AWAITING_RENTER);
        disputeMapper.insert(d);

        if (req.evidenceUrls() != null) {
            for (String url : req.evidenceUrls()) {
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                DisputeEvidence e = new DisputeEvidence();
                e.setDisputeId(d.getId());
                e.setImageUrl(url.trim());
                e.setEvidenceType("UPLOAD");
                e.setUploadedBy(initiatorId);
                evidenceMapper.insert(e);
            }
        }
        // TODO: UC-F1 扩展流程"引用取/还物存证"（BR-09）待订单域（窦源泽）就绪后接入
        return toResponse(d);
    }

    // ===== 我的纠纷（用户视角） =====

    public List<DisputeResponse> listMine(Long userId) {
        List<Dispute> list = disputeMapper.selectList(
                Wrappers.<Dispute>lambdaQuery()
                        .and(w -> w.eq(Dispute::getInitiatorId, userId)
                                .or().eq(Dispute::getRenterId, userId)
                                .or().eq(Dispute::getOwnerId, userId))
                        .orderByDesc(Dispute::getId)
        );
        return list.stream().map(this::toResponse).toList();
    }

    // ===== 租客回应物主扣留押金（同意立即和解 / 不同意进入调解） =====

    @Transactional
    public DisputeResponse respond(Long responderId, Long disputeId, RenterRespondRequest req) {
        Dispute d = requireDispute(disputeId);
        if (!d.getRenterId().equals(responderId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!AWAITING_RENTER.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATE_INVALID);
        }

        BigDecimal deposit = money(d.getDepositAmount());
        BigDecimal withhold = money(d.getWithholdAmount());
        String note = req.note() == null ? null : req.note().trim();

        if (Boolean.TRUE.equals(req.agree())) {
            // 同意：立即划转和解（扣留部分给物主，余下退回租客）
            executeFundTransfer(d, withhold, deposit.subtract(withhold), "和解");
            d.setRenterNote(note);
            d.setMediationConclusion("租客同意物主扣留押金，已和解");
            d.setStatus(SETTLED);
            disputeMapper.updateById(d);
            auditLogService.record(responderId, "DISPUTE_SETTLE", "DISPUTE", disputeId,
                    "租客同意扣留押金，纠纷和解");
        } else {
            d.setRenterNote(note);
            d.setStatus(MEDIATING);
            disputeMapper.updateById(d);
            auditLogService.record(responderId, "DISPUTE_DISAGREE", "DISPUTE", disputeId,
                    "租客不同意扣留押金，进入调解");
        }
        return toResponse(d);
    }

    // ===== 管理员队列（调解工作台 / 仲裁中心）=====

    public PageResult<DisputeResponse> listForAdmin(Long operatorId, String status, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));

        var wrapper = Wrappers.<Dispute>lambdaQuery()
                .eq(StringUtils.hasText(status), Dispute::getStatus, status)
                .orderByDesc(Dispute::getId);

        // BR-13：社区管理员仅可见本社区纠纷；系统管理员可见全部
        if (!PLATFORM_ADMIN.equals(AuthContext.require().role())) {
            long adminCommunityId = communityService.requireAdminCommunityId(operatorId);
            wrapper.eq(Dispute::getCommunityId, adminCommunityId);
        }

        Page<Dispute> p = new Page<>(safePage, safeSize);
        Page<Dispute> result = disputeMapper.selectPage(p, wrapper);
        return new PageResult<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize()
        );
    }

    // ===== 纠纷详情（当事人 / 管理员）=====

    public DisputeDetailResponse detail(Long viewerId, Long disputeId) {
        Dispute d = requireDispute(disputeId);
        requireViewer(viewerId, d);

        List<DisputeEvidenceResponse> evidences = evidenceMapper.selectList(
                        Wrappers.<DisputeEvidence>lambdaQuery()
                                .eq(DisputeEvidence::getDisputeId, disputeId)
                                .orderByAsc(DisputeEvidence::getId)
                ).stream()
                .map(e -> new DisputeEvidenceResponse(
                        e.getId(), e.getImageUrl(), e.getEvidenceType(), e.getUploadedBy(), e.getCreatedAt()))
                .toList();

        List<DisputeMessageResponse> messages = messageMapper.selectList(
                        Wrappers.<DisputeMessage>lambdaQuery()
                                .eq(DisputeMessage::getDisputeId, disputeId)
                                .orderByAsc(DisputeMessage::getId)
                ).stream()
                .map(m -> new DisputeMessageResponse(
                        m.getId(), m.getSenderId(), userName(m.getSenderId()), m.getSenderRole(),
                        m.getContent(), m.getCreatedAt()))
                .toList();

        AwardResponse award = findAward(d);

        return new DisputeDetailResponse(
                d.getId(), d.getOrderId(), d.getCommunityId(), communityName(d.getCommunityId()),
                d.getInitiatorId(), userName(d.getInitiatorId()),
                d.getRenterId(), userName(d.getRenterId()),
                d.getOwnerId(), userName(d.getOwnerId()),
                d.getReason(), money(d.getDepositAmount()), money(d.getRentAmount()),
                d.getStatus(), stageOf(d.getStatus()),
                d.getMediationConclusion(), d.getEscalateNote(),
                money(d.getWithholdAmount()), d.getRenterNote(),
                evidences, messages, award,
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    // ===== UC-F2 调解留言 =====

    @Transactional
    public DisputeMessageResponse mediate(Long operatorId, Long disputeId, MediateRequest req) {
        Dispute d = requireDisputeForAdmin(disputeId);
        if (!MEDIATING.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATE_INVALID);
        }
        String role = AuthContext.require().role();
        DisputeMessage m = new DisputeMessage();
        m.setDisputeId(disputeId);
        m.setSenderId(operatorId);
        m.setSenderRole(role == null ? "USER" : role);
        m.setContent(req.content().trim());
        messageMapper.insert(m);
        auditLogService.record(operatorId, "MEDIATE", "DISPUTE", disputeId, "调解留言");
        return new DisputeMessageResponse(
                m.getId(), m.getSenderId(), userName(m.getSenderId()), m.getSenderRole(),
                m.getContent(), m.getCreatedAt());
    }

    // ===== UC-F3 标记调解成功 =====

    @Transactional
    public void resolve(Long operatorId, Long disputeId, ResolveRequest req) {
        Dispute d = requireDisputeForAdmin(disputeId);
        if (!MEDIATING.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATE_INVALID);
        }
        d.setMediationConclusion(req.conclusion().trim());
        d.setStatus(RESOLVED);
        disputeMapper.updateById(d);

        // UC-F3 后置：调解成功解除押金冻结，全额退回租客（协商一致，无惩罚性扣除）
        BigDecimal deposit = money(d.getDepositAmount());
        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            walletService.unfreeze(d.getRenterId(), deposit, "DISPUTE", disputeId, "调解成功退回押金");
        }
        // TODO: 订单状态恢复（恢复纠纷前阶段）待订单域状态机就绪后接入

        auditLogService.record(operatorId, "RESOLVE_DISPUTE", "DISPUTE", disputeId,
                "标记调解成功：" + req.conclusion().trim());
    }

    // ===== UC-F4 上报仲裁 =====

    @Transactional
    public void escalate(Long operatorId, Long disputeId, EscalateRequest req) {
        Dispute d = requireDisputeForAdmin(disputeId);
        if (!MEDIATING.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATE_INVALID);
        }
        d.setEscalateNote(req.note().trim());
        d.setEscalatedAt(LocalDateTime.now());
        d.setStatus(PENDING_ARBITRATION);
        disputeMapper.updateById(d);
        auditLogService.record(operatorId, "ESCALATE_DISPUTE", "DISPUTE", disputeId,
                "上报仲裁：" + req.note().trim());
    }

    // ===== UC-F5 / UC-F6 仲裁裁决并划转押金 =====

    @Transactional
    public AwardResponse arbitrate(Long operatorId, Long disputeId, ArbitrateRequest req) {
        Dispute d = requireDispute(disputeId);
        if (!PENDING_ARBITRATION.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATE_INVALID);
        }
        Long existing = arbitrationMapper.selectCount(
                Wrappers.<DisputeArbitration>lambdaQuery().eq(DisputeArbitration::getDisputeId, disputeId));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.ARBITRATION_EXISTS);
        }

        BigDecimal deposit = money(d.getDepositAmount());
        BigDecimal deduct = money(req.deductAmount());
        String verdictType = req.verdictType();

        BigDecimal toLender;
        BigDecimal toBorrower;
        switch (verdictType) {
            case REFUND_ALL -> {
                toLender = BigDecimal.ZERO;
                toBorrower = deposit;
            }
            case DEDUCT_ALL -> {
                toLender = deposit;
                toBorrower = BigDecimal.ZERO;
            }
            case PARTIAL_DEDUCT -> {
                if (deduct.compareTo(BigDecimal.ZERO) <= 0 || deduct.compareTo(deposit) > 0) {
                    throw new BusinessException(ErrorCode.DEDUCT_EXCEEDS_DEPOSIT);
                }
                toLender = deduct;
                toBorrower = deposit.subtract(deduct);
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "未知裁决类型");
        }

        // BR-06/BR-21：资金划转（依赖订单域的押金冻结，见 UC-D2）
        executeFundTransfer(d, toLender, toBorrower, "仲裁");

        DisputeArbitration arb = new DisputeArbitration();
        arb.setDisputeId(disputeId);
        arb.setArbitratorId(operatorId);
        arb.setVerdictType(verdictType);
        arb.setDeductAmount(deduct);
        arb.setAwardText(req.awardReason().trim());
        arbitrationMapper.insert(arb);

        d.setStatus(ARBITRATED);
        disputeMapper.updateById(d);
        auditLogService.record(operatorId, "ARBITRATE", "DISPUTE", disputeId,
                "仲裁裁决：" + verdictType + "，扣除 ¥" + deduct);

        return toAwardResponse(arb, d);
    }

    // ===== 裁决书（UC-F6，双方可见）=====

    public AwardResponse getAward(Long viewerId, Long disputeId) {
        Dispute d = requireDispute(disputeId);
        requireViewer(viewerId, d);
        AwardResponse award = findAward(d);
        if (award == null) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_FOUND);
        }
        return award;
    }

    // ===== 内部工具 =====

    private void executeFundTransfer(Dispute d, BigDecimal toLender, BigDecimal toBorrower, String scene) {
        // 押金冻结在租客钱包；按裁决/和解解冻退回 / 扣除划付出租者
        if (toLender.compareTo(BigDecimal.ZERO) > 0) {
            walletService.consumeFrozen(d.getRenterId(), toLender, "DISPUTE", d.getId(), scene + "扣除押金划付出租者");
            walletService.income(d.getOwnerId(), toLender, "DISPUTE", d.getId(), scene + "押金收入");
        }
        if (toBorrower.compareTo(BigDecimal.ZERO) > 0) {
            walletService.unfreeze(d.getRenterId(), toBorrower, "DISPUTE", d.getId(), scene + "退回押金");
        }
    }

    private Dispute requireDispute(Long disputeId) {
        Dispute d = disputeMapper.selectById(disputeId);
        if (d == null) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_FOUND);
        }
        return d;
    }

    /**
     * 管理员访问纠纷：系统管理员可操作任意；社区管理员仅限本社区（BR-13）。
     */
    private Dispute requireDisputeForAdmin(Long disputeId) {
        Dispute d = requireDispute(disputeId);
        AuthUser auth = AuthContext.require();
        if (!PLATFORM_ADMIN.equals(auth.role())) {
            long adminCommunityId = communityService.requireAdminCommunityId(auth.id());
            if (!d.getCommunityId().equals(adminCommunityId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        return d;
    }

    private void requireViewer(Long viewerId, Dispute d) {
        if (d.getInitiatorId().equals(viewerId) || d.getRenterId().equals(viewerId) || d.getOwnerId().equals(viewerId)) {
            return;
        }
        AuthUser auth = AuthContext.require();
        if (PLATFORM_ADMIN.equals(auth.role())) {
            return;
        }
        if (COMMUNITY_ADMIN.equals(auth.role())) {
            Long admin = communityMemberMapper.selectCount(
                    Wrappers.<CommunityMember>lambdaQuery()
                            .eq(CommunityMember::getUserId, viewerId)
                            .eq(CommunityMember::getCommunityId, d.getCommunityId())
                            .eq(CommunityMember::getRole, "ADMIN")
                            .eq(CommunityMember::getStatus, "ACTIVE")
            );
            if (admin != null && admin > 0) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private AwardResponse findAward(Dispute d) {
        DisputeArbitration arb = arbitrationMapper.selectOne(
                Wrappers.<DisputeArbitration>lambdaQuery()
                        .eq(DisputeArbitration::getDisputeId, d.getId())
                        .orderByDesc(DisputeArbitration::getId)
                        .last("LIMIT 1")
        );
        return arb == null ? null : toAwardResponse(arb, d);
    }

    private AwardResponse toAwardResponse(DisputeArbitration arb, Dispute d) {
        BigDecimal deposit = money(d.getDepositAmount());
        BigDecimal deduct = money(arb.getDeductAmount());
        BigDecimal toLender;
        BigDecimal toBorrower;
        switch (arb.getVerdictType()) {
            case DEDUCT_ALL -> {
                toLender = deposit;
                toBorrower = BigDecimal.ZERO;
            }
            case PARTIAL_DEDUCT -> {
                toLender = deduct;
                toBorrower = deposit.subtract(deduct);
            }
            default -> {
                toLender = BigDecimal.ZERO;
                toBorrower = deposit;
            }
        }
        long counterpartyId = d.getInitiatorId().equals(d.getRenterId()) ? d.getOwnerId() : d.getRenterId();
        return new AwardResponse(
                arb.getId(),
                d.getOrderId(),
                userName(d.getInitiatorId()),
                userName(counterpartyId),
                deposit,
                verdictLabel(arb.getVerdictType()),
                toLender,
                toBorrower,
                arb.getAwardText(),
                arb.getCreatedAt(),
                userName(arb.getArbitratorId())
        );
    }

    private DisputeResponse toResponse(Dispute d) {
        return new DisputeResponse(
                d.getId(), d.getOrderId(), d.getCommunityId(), communityName(d.getCommunityId()),
                d.getInitiatorId(), userName(d.getInitiatorId()),
                d.getRenterId(), userName(d.getRenterId()),
                d.getOwnerId(), userName(d.getOwnerId()),
                d.getReason(), money(d.getDepositAmount()), money(d.getRentAmount()),
                money(d.getWithholdAmount()), d.getRenterNote(),
                d.getStatus(), stageOf(d.getStatus()),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }

    private String communityName(Long communityId) {
        if (communityId == null) {
            return null;
        }
        Community c = communityMapper.selectById(communityId);
        return c == null ? null : c.getName();
    }

    private String stageOf(String status) {
        if (PENDING_ARBITRATION.equals(status) || ARBITRATED.equals(status)) {
            return "arbitration";
        }
        return "mediation";
    }

    private String verdictLabel(String verdictType) {
        return switch (verdictType) {
            case REFUND_ALL -> "全退租客";
            case PARTIAL_DEDUCT -> "部分扣除";
            case DEDUCT_ALL -> "全额扣除给出租者";
            default -> verdictType;
        };
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
