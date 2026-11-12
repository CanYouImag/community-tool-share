-- 邻租：订单域最小建表脚本（供纠纷域演示闭环）
-- 说明：本文件为「发起纠纷」所需的订单载体（最小版），由钟姝伶临时补充；
--       完整订单域（预约/支付/交接/归还/评价状态机）由设计员窦源泽设计并替换本文件。

SET NAMES utf8mb4;

USE linzu;

CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id BIGINT NOT NULL COMMENT '物品 ID（物品域）',
    item_title VARCHAR(128) NOT NULL COMMENT '物品名称快照',
    community_id BIGINT NOT NULL COMMENT '所属社区 ID',
    renter_id BIGINT NOT NULL COMMENT '租客用户 ID',
    owner_id BIGINT NOT NULL COMMENT '出租者用户 ID',
    deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '押金快照',
    rent_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '租金快照',
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT '订单状态（最小占位，完整状态机待设计员补充）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_order_renter (renter_id),
    KEY idx_order_owner (owner_id),
    KEY idx_order_community (community_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单（最小载体）';

-- 种子订单：101/102 对应已有演示纠纷，201 供新发起纠纷演示
INSERT IGNORE INTO t_order (id, item_id, item_title, community_id, renter_id, owner_id, deposit_amount, rent_amount, status) VALUES
    (101, 1001, '18V 充电式电钻', 1, 2, 1, 50.00, 30.00, 'COMPLETED'),
    (102, 1002, '铝合金折叠梯', 2, 2, 1, 80.00, 40.00, 'COMPLETED'),
    (201, 1003, '冲击钻', 1, 2, 1, 60.00, 35.00, 'COMPLETED');

-- 押金冻结占位（UC-D2）：下单支付时租客押金转入冻结；此处以种子补齐演示闭环。
-- 说明：完整「下单→支付→冻结押金→解冻/扣划」状态机由订单域（窦源泽）实现，此处仅让演示数据自洽。
-- 冻结需覆盖三个种子订单的押金（初始均未发起纠纷，押金处于冻结、等待物主验收/扣留）：订单 101 押金 50 + 订单 102 押金 80 + 订单 201 押金 60 = 190
-- 注意：本行在 db 初始化阶段执行，此刻演示用户钱包尚未由后端 DemoDataInitializer 创建，
--       故实际冻结基线需在演示库中手工补齐（UC-D2 落地前由配置经理维护，见 docs/configuration-management）。
UPDATE t_wallet SET frozen_balance = frozen_balance + 190.00 WHERE user_id = 2;
