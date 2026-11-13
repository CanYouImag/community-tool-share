-- 评价、取还物存证、逾期计费（测试报告 D003 / D006 / D008 / D054）
USE linzu;

ALTER TABLE t_order
  ADD COLUMN overdue_days INT NOT NULL DEFAULT 0 COMMENT '逾期天数',
  ADD COLUMN overdue_fee DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '逾期费',
  ADD COLUMN overdue_penalty_applied TINYINT NOT NULL DEFAULT 0 COMMENT '逾期信用分扣减是否已执行';

CREATE TABLE IF NOT EXISTS t_order_evidence (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  stage VARCHAR(16) NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  uploaded_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_order_evidence_order (order_id, stage)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单取还物凭证';

CREATE TABLE IF NOT EXISTS t_order_evaluation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  lessor_id BIGINT NOT NULL,
  renter_id BIGINT NOT NULL,
  evaluator_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  content VARCHAR(500) DEFAULT NULL,
  image_urls VARCHAR(1000) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_evaluation_order_user (order_id, evaluator_id),
  KEY idx_evaluation_item (item_id),
  KEY idx_evaluation_target (target_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单评价';
