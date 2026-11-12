<script setup>
defineProps({
  modelValue: { type: String, required: true },
  tabs: { type: Array, required: true },
  label: { type: String, default: '页内功能' },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue']);
</script>

<template>
  <div class="view-tabs" role="group" :aria-label="label">
    <button
      v-for="tab in tabs"
      :key="tab.id"
      class="view-tab"
      type="button"
      :aria-pressed="modelValue === tab.id"
      :disabled="disabled"
      @click="tab.id !== modelValue && emit('update:modelValue', tab.id)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>
