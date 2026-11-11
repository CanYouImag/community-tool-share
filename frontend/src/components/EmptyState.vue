<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' },
});

const emit = defineEmits(['action']);
</script>

<template>
  <div class="empty">
    <div class="result-mark">空</div>
    <p>{{ text }}</p>
    <div v-if="actionText" class="actions">
      <a class="btn primary" @click="emit('action')">{{ actionText }}</a>
    </div>
  </div>
</template>
