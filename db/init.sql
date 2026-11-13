-- =====================================================================
-- 邻租（community-tool-share）数据库初始化基线
-- 说明：
-- 1. 本文件合并全部域 DDL（user / wallet / item / order），是唯一的
--    数据库初始化入口，执行方式：mysql -uroot -p < db/init.sql
-- 2. 各域单独脚本（db/user-wallet.sql、db/item.sql、db/order.sql）
--    与本文件保持同步，仅供分域评审与增量执行参考。
-- 3. 规范：utf8mb4、snake_case、统一基础字段 id/created_at/updated_at/deleted。
-- =====================================================================
CREATE DATABASE IF NOT EXISTS linzu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE linzu;

-- =====================================================================
-- 用户域（user）
-- =====================================================================
CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(32) NOT NULL COMMENT '登录名',
  phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  nickname VARCHAR(32) DEFAULT NULL COMMENT '昵称',
  avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  bio VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
  role VARCHAR(32) NOT NULL DEFAULT 'REGISTERED' COMMENT 'REGISTERED/CERTIFIED/COMMUNITY_ADMIN/PLATFORM_ADMIN',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/BANNED',
  credit_score INT NOT NULL DEFAULT 100 COMMENT '信用分',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_phone (phone),
  KEY idx_user_role (role),
  KEY idx_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE IF NOT EXISTS t_user_real_name (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  real_name VARCHAR(32) NOT NULL COMMENT '真实姓名',
  id_card_no VARCHAR(128) NOT NULL COMMENT '身份证号（AES-GCM 密文）',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/VERIFIED/REJECTED',
  reject_reason VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_real_name_user (user_id),
  KEY idx_real_name_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户实名认证';

-- =====================================================================
-- 钱包域（wallet）
-- =====================================================================
CREATE TABLE IF NOT EXISTS t_wallet (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  frozen_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  total_income DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '累计收入',
  version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户钱包';

CREATE TABLE IF NOT EXISTS t_wallet_transaction (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  type VARCHAR(32) NOT NULL COMMENT 'FREEZE/UNFREEZE/RENT_PAY/INCOME/COMMISSION/WITHDRAW/REFUND/ADMIN_ADJUST',
  amount DECIMAL(12, 2) NOT NULL COMMENT '变动金额，支出为负',
  balance_after DECIMAL(12, 2) NOT NULL COMMENT '变动后可用余额',
  ref_type VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型',
  ref_id BIGINT DEFAULT NULL COMMENT '关联业务 ID',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_wallet_tx_user_time (user_id, created_at),
  KEY idx_wallet_tx_type (type),
  KEY idx_wallet_tx_ref (ref_type, ref_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '钱包流水';

-- =====================================================================
-- 物品域（item）：分类（C1）、搜索筛选（C2）、详情（C3）、收藏（C4）、
-- 发布（C5）、审核（C6）、上下架/置顶/违规下架（C7）、图片
-- =====================================================================
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
  community_id BIGINT DEFAULT NULL COMMENT '所属社区 ID（社区域）',
  title VARCHAR(64) NOT NULL COMMENT '标题',
  description VARCHAR(2000) DEFAULT NULL COMMENT '描述',
  condition VARCHAR(64) NOT NULL COMMENT '成色说明（自由文本，需求基线 UC-C5）',
  rent_per_day DECIMAL(10, 2) NOT NULL COMMENT '日租金',
  deposit DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '押金',
  location VARCHAR(128) DEFAULT NULL COMMENT '取还地点（可选）',
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

-- =====================================================================
-- 订单域（order）：创建（D1）、支付/押金（D2）、接单（D3）、取物码（D4）、
-- 归还（D5）、验收（D6）、结算（D7）、取消（D8）、超时（D9）、留言（D10）
-- 状态机：PENDING_PAYMENT → PENDING_ACCEPT → PENDING_PICKUP → IN_USE →
--         PENDING_RETURN → PENDING_INSPECT → COMPLETED / CANCELLED / DISPUTED
-- =====================================================================
CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no VARCHAR(32) NOT NULL COMMENT '业务单号',
  item_id BIGINT NOT NULL COMMENT '物品 ID',
  item_title VARCHAR(64) NOT NULL COMMENT '下单时物品标题快照',
  item_image VARCHAR(255) DEFAULT NULL COMMENT '下单时物品封面图快照',
  community_id BIGINT DEFAULT NULL COMMENT '所属社区 ID（下单时从物品快照）',
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

-- =====================================================================
-- 种子数据：物品分类（C1）
-- =====================================================================
INSERT INTO t_item_category (name, sort, status) VALUES
  ('工具', 1, 'ENABLED'),
  ('家电', 2, 'ENABLED'),
  ('户外', 3, 'ENABLED'),
  ('数码', 4, 'ENABLED'),
  ('其他', 99, 'ENABLED');
