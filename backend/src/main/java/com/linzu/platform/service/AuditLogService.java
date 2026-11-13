package com.linzu.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linzu.common.api.PageResult;
import com.linzu.platform.dto.AuditLogResponse;
import com.linzu.platform.entity.AuditLog;
import com.linzu.platform.mapper.AuditLogMapper;
import com.linzu.user.entity.User;
import com.linzu.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    /** 记录管理员操作日志（BR-14，只增不删）。 */
    public void record(Long operatorId, String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

    public PageResult<AuditLogResponse> list(String action, Long operatorId, long page, long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));

        Page<AuditLog> p = new Page<>(safePage, safeSize);
        Page<AuditLog> result = auditLogMapper.selectPage(p,
                Wrappers.<AuditLog>lambdaQuery()
                        .eq(StringUtils.hasText(action), AuditLog::getAction, action)
                        .eq(operatorId != null, AuditLog::getOperatorId, operatorId)
                        .orderByDesc(AuditLog::getId)
        );
        List<AuditLogResponse> records = result.getRecords().stream().map(log -> new AuditLogResponse(
                log.getId(),
                log.getOperatorId(),
                userName(log.getOperatorId()),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getCreatedAt()
        )).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }
}
