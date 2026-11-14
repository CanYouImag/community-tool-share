-- 演示默认改为人工审核入社申请；需要自动通过时可在平台管理端勾选社区配置。
UPDATE t_community
SET auto_approve = 0
WHERE id = 2;
