-- 邻租：订单域建表脚本（D1-D10）
-- 说明：与 db/init.sql 基线保持同步；增量执行方式：mysql -uroot -p < db/order.sql
-- 状态机：PENDING_PAYMENT → PENDING_ACCEPT → PENDING_PICKUP → IN_USE →
--         PENDING_RETURN → PENDING_INSPECT → COMPLETED / CANCELLED / DISPUTED
USE linzu;

CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no VARCHAR(32) NOT NULL COMMENT '业务单号',
  item_id BIGINT NOT NULL COMMENT '物品 ID',
  item_title VARCHAR(64) NOT NULL COMMENT '下单时物品标题快照',
  item_image VARCHAR(255) DEFAULT NULL COMMENT '下单时物品封面图快照',
  lessor_id BIGINT NOT NULL COMMENT '出租人用户 ID',
  renter_id BIGINT NOT NULL COMMENT '租客用户 ID',
  start_date DATE NOT NULL COMMENT '租期开始日期',
  end_date DATE NOT NULL COMMENT '租期结束日期',
  days INT NOT NULL COMMENT '租期天数',
  rent_amount DECIMAL(12, 2) NOT NULL COMMENT '租金总额',
  deposit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '押金',
  commission_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '平台佣金，结算时确定',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT 'PENDING_PAYMENT/PENDING_ACCEPT/PENDING_PICKUP/IN_USE/PENDING_RETURN/PENDING_INSPECT/COMPLETED/CANCELLED/DISPUTED',
  pickup_code VARCHAR(6) DEFAULT NULL COMMENT '取物码，验码取物后置空',
  cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消/纠纷原因',
  paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
  accepted_at DATETIME DEFAULT NULL COMMENT '接单时间',
  pickup_at DATETIME DEFAULT NULL COMMENT '取物时间',
  return_at DATETIME DEFAULT NULL COMMENT '发起归还时间',
  completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
  cancelled_at DATETIME DEFAULT NULL COMMENT '取消时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_item (item_id),
  KEY idx_order_lessor (lessor_id, status),
  KEY idx_order_renter (renter_id, status),
  KEY idx_order_status_updated (status, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '租赁订单';

CREATE TABLE IF NOT EXISTS t_order_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单 ID',
  sender_id BIGINT NOT NULL COMMENT '发送人用户 ID',
  content VARCHAR(500) NOT NULL COMMENT '留言内容',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_message_order (order_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单留言';
