// 业务字典常量：状态枚举与文案统一在此维护，避免页面散落魔法字符串

// 订单状态（与需求附录 B 状态机对齐）
export const ORDER_STATUS = {
  PENDING: { label: '待确认', cls: 'warn' },
  PAID: { label: '确认支付', cls: 'blue' },
  TO_PICKUP: { label: '待取物', cls: 'blue' },
  RENTING: { label: '租用中', cls: 'neutral' },
  OVERDUE: { label: '逾期', cls: 'error' },
  TO_RETURN: { label: '待归还确认', cls: 'warn' },
  COMPLETED: { label: '已完成', cls: '' },
  CANCELLED: { label: '已取消', cls: 'neutral' },
  DISPUTING: { label: '纠纷中', cls: 'error' },
  CLOSED: { label: '已终结', cls: 'neutral' }
}

// 订单状态筛选项（下拉）
export const ORDER_STATUS_FILTER = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待确认' },
  { value: 'TO_PICKUP', label: '待取物' },
  { value: 'RENTING', label: '租用中' },
  { value: 'TO_RETURN', label: '待归还确认' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'DISPUTING', label: '纠纷中' },
  { value: 'CLOSED', label: '已终结' }
]

// 纠纷状态
export const DISPUTE_STATUS = {
  MEDIATING: { label: '调解中', cls: 'warn' },
  PENDING_ARBITRATION: { label: '待仲裁', cls: 'error' },
  RESOLVED: { label: '已调解', cls: '' },
  ARBITRATED: { label: '已裁决', cls: 'blue' }
}

// 资金流水类型
export const FLOW_TYPE = {
  INCOME: { label: '收入', sign: '+' },
  EXPENSE: { label: '支出', sign: '−' },
  FREEZE: { label: '冻结', sign: '' },
  UNFREEZE: { label: '解冻', sign: '+' }
}

// 流水类型筛选项
export const FLOW_TYPE_FILTER = [
  { value: '', label: '全部' },
  { value: 'INCOME', label: '收入' },
  { value: 'EXPENSE', label: '支出' },
  { value: 'FREEZE', label: '冻结' },
  { value: 'UNFREEZE', label: '解冻' }
]

// 物品分类
export const ITEM_CATEGORY = ['工具', '家电', '户外', '数码', '其他']

// 社区加入状态
export const MEMBER_STATUS = {
  JOINED: { label: '已加入', cls: '' },
  PENDING: { label: '待审核', cls: 'warn' }
}

// 举报对象类型
export const REPORT_TARGET = ['物品', '用户', '评价']

// 举报类型
export const REPORT_TYPE = ['信息不实', '违规内容', '恶意评价', '其他']

// 平台佣金比例（与 BR-07 一致，后台可配置）
export const COMMISSION_RATE = 0.1

// 逾期计费倍率（BR-08）
export const OVERDUE_RATE = 1.5
