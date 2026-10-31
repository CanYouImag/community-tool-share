// 通用格式化工具

// 金额格式化：1234.5 -> ¥1,234.50
export function formatMoney(value) {
  const n = Number(value || 0)
  const sign = n < 0 ? '−' : ''
  const abs = Math.abs(n)
  return `${sign}¥${abs.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`
}

// 纯金额数字（不带 ¥ 符号），用于价格大数字展示
export function formatAmount(value) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 日期格式化：Date/字符串 -> YYYY-MM-DD
export function formatDate(value) {
  if (!value) return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 日期时间格式化：YYYY-MM-DD HH:mm
export function formatDateTime(value) {
  if (!value) return ''
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${formatDate(d)} ${hh}:${mm}`
}
