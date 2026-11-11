-- 邻租：社区与治理域建表脚本（社区 + 纠纷 + 平台）
-- 说明：本文件由钟姝伶维护，最终由窦源泽合并进 db/init.sql。
-- 执行方式：mysql -uroot -p < db/community-dispute-platform.sql
-- 当前进度：社区域已就绪；纠纷域、平台域表结构随对应模块陆续补充。

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
    (1, '青禾社区', '取物和归还时请核对配件并拍照。', NULL, 0, 0, 0, 'ACTIVE'),
    (2, '梧桐里社区', '欢迎新邻居加入，共享闲置工具。', NULL, 0, 0, 0, 'ACTIVE'),
    (3, '松柏社区', '本周六社区置换活动，欢迎参加。', NULL, 1, 0, 0, 'ACTIVE');
