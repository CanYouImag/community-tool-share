// 业务字典常量：状态枚举与文案统一在此维护，避免页面散落魔法字符串

// 订单状态（对齐设计文档 OrderStateEnum，文案保留原型用户视角）
// 状态流转：PENDING_PAYMENT → PENDING_ACCEPT → PENDING_PICKUP → IN_USE
//          → PENDING_RETURN → PENDING_INSPECT → COMPLETED
// 旁路：CANCELLED（已取消）、DISPUTED（纠纷中）、OVERDUE（逾期）、CLOSED（已终结）
export const ORDER_STATUS = {
  PENDING_PAYMENT: { label: '待确认', cls: 'warn' },
  PENDING_ACCEPT: { label: '确认支付', cls: 'blue' },
  PENDING_PICKUP: { label: '待取物', cls: 'blue' },
  IN_USE: { label: '租用中', cls: 'neutral' },
  PENDING_RETURN: { label: '待归还确认', cls: 'warn' },
  PENDING_INSPECT: { label: '待验收', cls: 'warn' },
  COMPLETED: { label: '已完成', cls: '' },
  CANCELLED: { label: '已取消', cls: 'neutral' },
  DISPUTED: { label: '纠纷中', cls: 'error' },
  OVERDUE: { label: '逾期', cls: 'error' },
  CLOSED: { label: '已终结', cls: 'neutral' },
};

// 订单状态筛选项（下拉）
export const ORDER_STATUS_FILTER = [
  { value: '', label: '全部状态' },
  { value: 'PENDING_PAYMENT', label: '待确认' },
  { value: 'PENDING_PICKUP', label: '待取物' },
  { value: 'IN_USE', label: '租用中' },
  { value: 'PENDING_RETURN', label: '待归还确认' },
  { value: 'PENDING_INSPECT', label: '待验收' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'DISPUTED', label: '纠纷中' },
  { value: 'OVERDUE', label: '逾期' },
  { value: 'CLOSED', label: '已终结' },
];

// 纠纷状态（两级机制：社区调解 → 平台仲裁，对齐设计文档「社区治理子系统」）
export const DISPUTE_STATUS = {
  AWAITING_RENTER: { label: '待租客回应', cls: 'blue' },
  MEDIATING: { label: '调解中', cls: 'warn' },
  PENDING_ARBITRATION: { label: '待仲裁', cls: 'error' },
  RESOLVED: { label: '已调解', cls: '' },
  ARBITRATED: { label: '已裁决', cls: 'blue' },
  SETTLED: { label: '已和解', cls: '' },
};

// 资金流水类型
export const FLOW_TYPE = {
  INCOME: { label: '收入', sign: '+' },
  EXPENSE: { label: '支出', sign: '−' },
  FREEZE: { label: '冻结', sign: '' },
  UNFREEZE: { label: '解冻', sign: '+' },
};

// 流水类型筛选项
export const FLOW_TYPE_FILTER = [
  { value: '', label: '全部' },
  { value: 'INCOME', label: '收入' },
  { value: 'EXPENSE', label: '支出' },
  { value: 'FREEZE', label: '冻结' },
  { value: 'UNFREEZE', label: '解冻' },
];

// 物品分类
export const ITEM_CATEGORY = ['工具', '家电', '户外', '数码', '其他'];

// 社区加入状态（成员关系）
export const MEMBER_STATUS = {
  JOINED: { label: '已加入', cls: '' },
  PENDING: { label: '待审核', cls: 'warn' },
  REJECTED: { label: '已拒绝', cls: 'neutral' },
};

// 社区申请审核结果（对齐设计文档 CommunityApplyDO，管理员侧审核）
export const COMMUNITY_APPLY_STATUS = {
  PENDING: { label: '待审核', cls: 'warn' },
  PASSED: { label: '已通过', cls: '' },
  REJECTED: { label: '已拒绝', cls: 'neutral' },
};

// 举报对象类型
export const REPORT_TARGET = ['物品', '用户', '评价'];

// 举报类型
export const REPORT_TYPE = ['信息不实', '违规内容', '恶意评价', '其他'];

// 平台佣金比例（与 BR-07 一致，后台可配置）
export const COMMISSION_RATE = 0.1;

// 逾期计费倍率（BR-08）
export const OVERDUE_RATE = 1.5;

// 物品状态
export const ITEM_STATUS = {
  PENDING_REVIEW: { label: '待审核', cls: 'warn' },
  AVAILABLE: { label: '已上架', cls: '' },
  REJECTED: { label: '已驳回', cls: 'error' },
  PAUSED: { label: '暂停出租', cls: 'neutral' },
  REMOVED: { label: '已下架', cls: 'error' },
};
