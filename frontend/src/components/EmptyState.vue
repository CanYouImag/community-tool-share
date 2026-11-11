<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' },
});

const emit = defineEmits(['action']);
</script>

<template>
  <div class="empty">
    <p>{{ text }}</p>
    <div v-if="actionText" class="actions">
      <button type="button" class="btn primary" @click="emit('action')">{{ actionText }}</button>
    </div>
  </div>
</template>
