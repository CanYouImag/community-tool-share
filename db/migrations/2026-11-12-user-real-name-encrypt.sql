-- 2026-11-12 用户域升级：身份证号改为 AES-GCM 密文存储
-- 已有数据库执行本脚本；新库直接执行 db/user-wallet.sql 即可。
USE linzu;

ALTER TABLE t_user_real_name
  MODIFY COLUMN id_card_no VARCHAR(128) NOT NULL COMMENT '身份证号（AES-GCM 密文）';
