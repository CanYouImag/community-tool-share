-- 邻租：社区与治理域建表脚本（社区 + 纠纷 + 平台）
-- 说明：本文件由钟姝伶维护，最终由窦源泽合并进 db/init.sql。
-- 执行方式：mysql -uroot -p < db/community-dispute-platform.sql
-- 当前进度：社区域、纠纷域、平台域已就绪。

-- 强制客户端按 utf8mb4 读取本脚本，避免中文被 latin1 双重编码成乱码
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS linzu
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE linzu;

-- ===== 社区 =====

CREATE TABLE IF NOT EXISTS t_community (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '社区名称',
    intro VARCHAR(255) DEFAULT NULL COMMENT '社区简介',
    cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
    auto_approve TINYINT(1) NOT NULL DEFAULT 0 COMMENT '加入申请自动通过：0 审核 / 1 自动',
    member_count INT NOT NULL DEFAULT 0 COMMENT '成员数（冗余统计）',
    item_count INT NOT NULL DEFAULT 0 COMMENT '在租物品数（冗余统计）',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_community_name (name),
    KEY idx_community_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区';

CREATE TABLE IF NOT EXISTS t_community_member (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    community_id BIGINT NOT NULL COMMENT '社区 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT 'MEMBER/ADMIN',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LEFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_user_community (user_id, community_id),
    KEY idx_member_community (community_id),
    KEY idx_member_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区成员关系';

CREATE TABLE IF NOT EXISTS t_community_announcement (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    community_id BIGINT NOT NULL COMMENT '社区 ID',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '正文',
    image_url VARCHAR(255) DEFAULT NULL COMMENT '配图地址',
    created_by BIGINT NOT NULL COMMENT '发布人用户 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_announcement_community (community_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区公告';

CREATE TABLE IF NOT EXISTS t_community_join_apply (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    community_id BIGINT NOT NULL COMMENT '社区 ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户 ID',
    apply_reason VARCHAR(255) DEFAULT NULL COMMENT '申请理由',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    handled_by BIGINT DEFAULT NULL COMMENT '审核人用户 ID',
    handled_at DATETIME DEFAULT NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_apply_community_status (community_id, status),
    KEY idx_apply_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区加入申请';

-- ===== 演示种子数据（幂等，可重复执行）=====
INSERT IGNORE INTO t_community (id, name, intro, cover_url, auto_approve, member_count, item_count, status) VALUES
    (1, '青禾社区', '取物和归还时请核对配件并拍照。', NULL, 0, 1, 0, 'ACTIVE'),
    (2, '梧桐里社区', '欢迎新邻居加入，共享闲置工具。', NULL, 0, 1, 0, 'ACTIVE'),
    (3, '松柏社区', '本周六社区置换活动，欢迎参加。', NULL, 1, 0, 0, 'ACTIVE');

-- 社区成员：admin(id=1) 为青禾社区管理员，Starling(id=2) 为梧桐里成员
INSERT IGNORE INTO t_community_member (id, community_id, user_id, role, status) VALUES
    (1, 1, 1, 'ADMIN', 'ACTIVE'),
    (2, 2, 2, 'MEMBER', 'ACTIVE');

-- 社区公告（青禾社区）
INSERT IGNORE INTO t_community_announcement (id, community_id, title, content, created_by) VALUES
    (1, 1, '青禾社区公约', '借用工具请提前一天预约，归还时当面清点配件。', 1),
    (2, 1, '周末工具共享日', '本周六下午 2 点在社区活动室举办工具共享与维修小课堂。', 1);

-- 入社申请：Starling 申请加入青禾社区（待审核）
INSERT IGNORE INTO t_community_join_apply (id, community_id, user_id, apply_reason, status) VALUES
    (1, 1, 2, '新搬来青禾小区，想借用家用工具。', 'PENDING');

-- ===== 纠纷与举报 =====

CREATE TABLE IF NOT EXISTS t_dispute (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '订单 ID（订单域）',
    community_id BIGINT NOT NULL COMMENT '所属社区 ID',
    initiator_id BIGINT NOT NULL COMMENT '发起纠纷的用户 ID',
    renter_id BIGINT NOT NULL COMMENT '租客用户 ID（押金冻结在其钱包）',
    owner_id BIGINT NOT NULL COMMENT '出租者用户 ID',
    reason VARCHAR(500) NOT NULL COMMENT '物主提出扣留理由',
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '押金快照',
    rent_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '租金快照',
    withhold_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '物主提出扣留金额',
    renter_note VARCHAR(500) DEFAULT NULL COMMENT '租客回应说明（不同意时的异议理由）',
    status VARCHAR(32) NOT NULL DEFAULT 'AWAITING_RENTER' COMMENT 'AWAITING_RENTER/MEDIATING/PENDING_ARBITRATION/RESOLVED/ARBITRATED/SETTLED',
    mediation_conclusion VARCHAR(500) DEFAULT NULL COMMENT '调解结论（UC-F3）',
    escalate_note VARCHAR(500) DEFAULT NULL COMMENT '上报仲裁说明（UC-F4）',
    escalated_at DATETIME DEFAULT NULL COMMENT '上报仲裁时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_dispute_community_status (community_id, status),
    KEY idx_dispute_order (order_id),
    KEY idx_dispute_initiator (initiator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '纠纷单';

CREATE TABLE IF NOT EXISTS t_dispute_evidence (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dispute_id BIGINT NOT NULL COMMENT '纠纷 ID',
    image_url VARCHAR(255) NOT NULL COMMENT '凭证图片地址',
    evidence_type VARCHAR(16) NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/PICKUP/RETURN',
    uploaded_by BIGINT DEFAULT NULL COMMENT '上传人用户 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_evidence_dispute (dispute_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '纠纷凭证/举证';

CREATE TABLE IF NOT EXISTS t_dispute_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dispute_id BIGINT NOT NULL COMMENT '纠纷 ID',
    sender_id BIGINT NOT NULL COMMENT '发送人用户 ID',
    sender_role VARCHAR(24) NOT NULL DEFAULT 'USER' COMMENT 'USER/COMMUNITY_ADMIN/PLATFORM_ADMIN',
    content VARCHAR(500) NOT NULL COMMENT '留言内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_message_dispute (dispute_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '调解留言';

CREATE TABLE IF NOT EXISTS t_dispute_arbitration (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    dispute_id BIGINT NOT NULL COMMENT '纠纷 ID',
    arbitrator_id BIGINT NOT NULL COMMENT '裁决人（系统管理员）用户 ID',
    verdict_type VARCHAR(32) NOT NULL COMMENT 'REFUND_ALL/PARTIAL_DEDUCT/DEDUCT_ALL',
    deduct_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '扣除金额（部分扣除时）',
    award_text TEXT NOT NULL COMMENT '裁决书文本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_arbitration_dispute (dispute_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '仲裁裁决与裁决书';

CREATE TABLE IF NOT EXISTS t_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    reporter_id BIGINT NOT NULL COMMENT '举报人用户 ID',
    community_id BIGINT DEFAULT NULL COMMENT '关联社区 ID（BR-13 行级隔离；物品域就绪后可自动推导）',
    target_type VARCHAR(16) NOT NULL COMMENT 'ITEM/USER/REVIEW',
    target_id BIGINT DEFAULT NULL COMMENT '举报目标 ID',
    reason_type VARCHAR(32) NOT NULL COMMENT 'FALSE_INFO/ILLEGAL_CONTENT/MALICIOUS_REVIEW/OTHER',
    description VARCHAR(500) NOT NULL COMMENT '举报说明',
    evidence_urls VARCHAR(1000) DEFAULT NULL COMMENT '证据图片地址（逗号分隔）',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSED',
    handle_action VARCHAR(32) DEFAULT NULL COMMENT 'REJECT/ITEM_DOWN/WARN/BAN',
    handle_note VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
    handled_by BIGINT DEFAULT NULL COMMENT '处理人用户 ID',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_report_status (status),
    KEY idx_report_reporter (reporter_id),
    KEY idx_report_community (community_id, status),
    KEY idx_report_target (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '举报';

-- ===== 演示种子数据：纠纷与举报（order_id 为占位订单号，订单域就绪后替换为真实订单）=====
-- 说明：初始不预置纠纷——三个种子订单（101/102/201）押金均已冻结、尚未发起纠纷，
--       物主可从「提出扣留押金」从零发起，便于完整演示「提出 → 租客回应 → 调解 → 仲裁」闭环。

-- 举报（待处理）
INSERT IGNORE INTO t_report (id, reporter_id, community_id, target_type, target_id, reason_type, description) VALUES
    (1, 2, 1, 'ITEM', 101, 'FALSE_INFO', '该电钻详情页标注功率 800W，实际铭牌仅 500W，涉嫌虚假描述。'),
    (2, 2, 1, 'ITEM', 102, 'ILLEGAL_CONTENT', '该物品详情图片包含违规内容，请平台核实下架。');

-- ===== 平台治理 =====

CREATE TABLE IF NOT EXISTS t_platform_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    description VARCHAR(255) DEFAULT NULL COMMENT '说明',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '平台参数配置';

INSERT IGNORE INTO t_platform_config (config_key, config_value, description) VALUES
    ('commission_rate', '0.10', '平台佣金比例（0~0.5）'),
    ('community_auto_approve', 'false', '社区加入是否默认自动通过'),
    ('item_audit_enabled', 'true', '物品审核开关');

CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id BIGINT NOT NULL COMMENT '操作人用户 ID',
    action VARCHAR(64) NOT NULL COMMENT '动作类型',
    target_type VARCHAR(32) DEFAULT NULL COMMENT '目标类型',
    target_id BIGINT DEFAULT NULL COMMENT '目标 ID',
    detail VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_operator_time (operator_id, created_at),
    KEY idx_audit_action (action)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作审计日志（只增不删，BR-14）';
