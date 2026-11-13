-- 修复测试报告 D037：已完成订单缺少 completed_at
USE linzu;

UPDATE t_order
SET completed_at = COALESCE(completed_at, updated_at, created_at)
WHERE status = 'COMPLETED'
  AND completed_at IS NULL;
