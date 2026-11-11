<script setup>
import { computed } from 'vue';

const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(['change']);
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));

function go(p) {
  if (props.disabled || p < 1 || p > pageCount.value || p === props.page) return;
  emit('change', p);
}
</script>

<template>
  <div class="pagination">
    <span>共 {{ total }} 条</span>
    <div class="actions">
      <button
        class="btn small"
        type="button"
        :disabled="disabled || page <= 1"
        @click="go(page - 1)"
      >
        上一页
      </button>
      <span class="page-number" aria-live="polite">{{ page }} / {{ pageCount }}</span>
      <button
        class="btn small"
        type="button"
        :disabled="disabled || page >= pageCount"
        @click="go(page + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>
