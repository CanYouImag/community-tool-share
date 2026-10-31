<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: '' },
  // 支持直接传文案 + 颜色类（如自定义状态）
  label: { type: String, default: '' },
  cls: { type: String, default: '' }
})

const mapping = {
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
  MEDIATING: { label: '调解中', cls: 'warn' },
  PENDING_ARBITRATION: { label: '待仲裁', cls: 'error' },
  RESOLVED: { label: '已调解', cls: '' },
  ARBITRATED: { label: '已裁决', cls: 'blue' },
  JOINED: { label: '已加入', cls: '' },
  MEMBER_PENDING: { label: '待审核', cls: 'warn' },
  MEMBER_REJECTED: { label: '已拒绝', cls: 'neutral' },
  APPLY_PASSED: { label: '已通过', cls: '' },
  APPLY_REJECTED: { label: '已拒绝', cls: 'neutral' }
}

const display = computed(() => {
  if (props.label) return { label: props.label, cls: props.cls }
  return mapping[props.status] || { label: props.status || '—', cls: '' }
})
</script>

<template>
  <span class="badge" :class="display.cls">{{ display.label }}</span>
</template>
