export function localDateKey(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function isDateKey(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function shiftDate(dateKey, days) {
  if (!isDateKey(dateKey)) throw new Error('日期无效');
  const date = new Date(`${dateKey}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

export function monthCells(month) {
  if (!isDateKey(`${month}-01`)) throw new Error('月份无效');
  const first = new Date(`${month}-01T00:00:00Z`);
  const next = new Date(first);
  next.setUTCMonth(next.getUTCMonth() + 1);
  const count = Math.round((next - first) / 86400000);
  const offset = (first.getUTCDay() + 6) % 7;
  return Array.from({ length: Math.ceil((offset + count) / 7) * 7 }, (_, index) => {
    const day = index - offset + 1;
    return day > 0 && day <= count
      ? { day, date: `${month}-${String(day).padStart(2, '0')}` }
      : null;
  });
}

export function dateAvailability(date, ranges, today = localDateKey()) {
  if (
    ranges.some((range) => date >= range.startDate && (!range.endDate || date <= range.endDate))
  ) {
    return { code: 'busy', label: '已占用' };
  }
  return date < today ? { code: 'past', label: '已过去' } : { code: 'available', label: '可租' };
}
