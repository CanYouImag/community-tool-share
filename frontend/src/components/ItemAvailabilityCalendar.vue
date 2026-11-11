<script setup>
import { computed, ref } from 'vue';
import { dateAvailability, isDateKey, localDateKey, monthCells } from '@/utils/itemCalendar';

const props = defineProps({
  ranges: { type: Array, default: null },
});

const today = localDateKey();
const month = ref(today.slice(0, 7));
const weekdays = ['一', '二', '三', '四', '五', '六', '日'];
const monthLabel = computed(
  () => `${Number(month.value.slice(0, 4))} 年 ${Number(month.value.slice(5))} 月`,
);
const hasAvailability = computed(
  () =>
    Array.isArray(props.ranges) &&
    props.ranges.every(
      (range) =>
        isDateKey(range?.startDate) &&
        (range.endDate === null || (isDateKey(range.endDate) && range.endDate >= range.startDate)),
    ),
);
const weeks = computed(() => {
  const cells = monthCells(month.value);
  return Array.from({ length: cells.length / 7 }, (_, index) =>
    cells.slice(index * 7, index * 7 + 7),
  );
});
const hasOpenEndedBooking = computed(() => props.ranges?.some((range) => range.endDate === null));
const occupiedDays = computed(() =>
  hasAvailability.value
    ? monthCells(month.value).filter((cell) => cell && status(cell.date).code === 'busy').length
    : 0,
);

function changeMonth(offset) {
  const date = new Date(`${month.value}-01T00:00:00Z`);
  date.setUTCMonth(date.getUTCMonth() + offset);
  if (date.getUTCFullYear() < 1 || date.getUTCFullYear() > 9999) return;
  month.value = date.toISOString().slice(0, 7);
}

function status(date) {
  return dateAvailability(date, props.ranges, today);
}
</script>

<template>
  <section class="panel" aria-labelledby="availability-heading">
    <div class="panel-title">
      <h2 id="availability-heading">可租日历</h2>
    </div>

    <p v-if="!hasAvailability" role="status">暂无可租日期信息，请稍后再试。</p>
    <template v-else>
      <div class="calendar-navigation">
        <button
          class="btn small"
          type="button"
          :disabled="month === '0001-01'"
          @click="changeMonth(-1)"
        >
          上个月
        </button>
        <strong aria-live="polite">{{ monthLabel }}</strong>
        <button
          class="btn small"
          type="button"
          :disabled="month === '9999-12'"
          @click="changeMonth(1)"
        >
          下个月
        </button>
        <button class="btn small" type="button" @click="month = today.slice(0, 7)">本月</button>
      </div>

      <table class="availability-table" :aria-label="`${monthLabel}可租日期`">
        <thead>
          <tr>
            <th v-for="day in weekdays" :key="day" scope="col">{{ day }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(week, index) in weeks" :key="index">
            <td
              v-for="(cell, column) in week"
              :key="column"
              :class="cell ? status(cell.date).code : ''"
            >
              <span
                v-if="cell"
                :aria-label="`${cell.date}，${status(cell.date).label}`"
                :aria-current="cell.date === today ? 'date' : undefined"
                :title="`${cell.date}：${status(cell.date).label}`"
                >{{ cell.day }}</span
              >
            </td>
          </tr>
        </tbody>
      </table>

      <div class="calendar-legend" aria-label="日期状态说明">
        <span class="legend-available">可租</span>
        <span class="legend-busy">已占用（划线）</span>
        <span class="legend-past">已过去（浅色）</span>
      </div>
      <p class="help" aria-live="polite">
        {{ occupiedDays ? `本月有 ${occupiedDays} 天已占用。` : '本月暂无已占用日期。' }}
      </p>
      <p v-if="hasOpenEndedBooking" class="help" role="status">
        该物品有尚未结束的租赁，后续可租时间需待归还和验收后确认。
      </p>
      <p class="help">实际可租时间以下单时的校验结果为准。</p>
    </template>
  </section>
</template>

<style scoped>
.calendar-navigation {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.availability-table {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

.availability-table th,
.availability-table td {
  padding: 10px 2px;
  text-align: center;
}

.availability-table td span {
  display: inline-block;
  min-width: 28px;
}

.availability-table .busy {
  background: var(--color-rule);
  color: var(--color-ink);
}

/* 日期数字是 inline-block，划线必须直接应用到数字上。 */
.availability-table .busy > span {
  text-decoration: line-through;
  text-decoration-thickness: 2px;
}

.availability-table .past {
  background: var(--color-paper);
}

.availability-table .past > span {
  color: color-mix(in oklab, var(--color-muted) 70%, var(--color-surface));
}

.calendar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
}

.calendar-legend > span {
  padding: 3px 8px;
  border-radius: var(--radius-sm);
}

.legend-busy {
  background: var(--color-rule);
  color: var(--color-ink);
}

.legend-past {
  background: var(--color-paper);
  color: var(--color-muted);
}

.legend-available {
  background: var(--color-soft);
  color: var(--color-accent);
}

.availability-table .available {
  color: var(--color-accent);
}

.availability-table [aria-current='date'] {
  outline: 2px solid var(--color-accent);
  border-radius: var(--radius-sm);
  font-weight: 700;
}
</style>
