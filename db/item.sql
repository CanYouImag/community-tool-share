-- 邻租：物品域建表脚本（C1-C7）
-- 说明：与 db/init.sql 基线保持同步；增量执行方式：mysql -uroot -p < db/item.sql
USE linzu;

CREATE TABLE IF NOT EXISTS t_item_category (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(32) NOT NULL COMMENT '分类名称',
  sort INT NOT NULL DEFAULT 0 COMMENT '展示排序，越小越靠前',
  status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_name (name),
  KEY idx_category_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物品分类';

CREATE TABLE IF NOT EXISTS t_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id BIGINT NOT NULL COMMENT '分类 ID',
  owner_id BIGINT NOT NULL COMMENT '物主用户 ID',
  title VARCHAR(64) NOT NULL COMMENT '标题',
  description VARCHAR(2000) DEFAULT NULL COMMENT '描述',
  condition_level VARCHAR(16) NOT NULL COMMENT '成色：NEW/LIKE_NEW/GOOD/FAIR',
  rent_per_day DECIMAL(10, 2) NOT NULL COMMENT '日租金',
  deposit DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '押金',
  location VARCHAR(128) NOT NULL COMMENT '所在社区/取还地点',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/REJECTED/LISTED/DELISTED/VIOLATION_OFF',
  is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '平台置顶：1 是 0 否',
  reject_reason VARCHAR(255) DEFAULT NULL COMMENT '审核驳回原因',
  violation_reason VARCHAR(255) DEFAULT NULL COMMENT '违规下架原因',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_item_category (category_id),
  KEY idx_item_owner (owner_id),
  KEY idx_item_status_pinned (status, is_pinned),
  KEY idx_item_rent_per_day (rent_per_day)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物品';

CREATE TABLE IF NOT EXISTS t_item_image (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_id BIGINT NOT NULL COMMENT '物品 ID',
  url VARCHAR(255) NOT NULL COMMENT '图片地址',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序，0 为封面',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_item_image_item (item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物品图片';

CREATE TABLE IF NOT EXISTS t_item_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  item_id BIGINT NOT NULL COMMENT '物品 ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_user_item (user_id, item_id),
  KEY idx_favorite_item (item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物品收藏';

INSERT INTO t_item_category (name, sort, status) VALUES
  ('电动工具', 1, 'ENABLED'),
  ('手动工具', 2, 'ENABLED'),
  ('户外装备', 3, 'ENABLED'),
  ('厨房电器', 4, 'ENABLED'),
  ('清洁设备', 5, 'ENABLED'),
  ('母婴用品', 6, 'ENABLED'),
  ('数码配件', 7, 'ENABLED'),
  ('其他', 99, 'ENABLED');
