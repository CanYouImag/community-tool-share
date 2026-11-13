-- 修复测试报告 D002：流水 amount 统一表示总资产（balance + frozen）变化
-- 冻结/解冻只是余额内部划转，不应计入总资产变化。
USE linzu;

UPDATE t_wallet_transaction
SET amount = 0.00
WHERE type IN ('FREEZE', 'UNFREEZE');
