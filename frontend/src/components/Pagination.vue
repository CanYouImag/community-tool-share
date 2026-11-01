<script setup>
defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 }
})

const emit = defineEmits(['change'])

function go(p) {
  emit('change', p)
}
</script>

<template>
  <div class="pagination">
    <span>共 {{ total }} 条</span>
    <div class="actions">
      <a class="btn small disabled" v-if="page <= 1">上一页</a>
      <a v-else class="btn small" @click="go(page - 1)">上一页</a>
      <span class="page-number">{{ page }}</span>
      <a class="btn small disabled" v-if="page * pageSize >= total">下一页</a>
      <a v-else class="btn small" @click="go(page + 1)">下一页</a>
    </div>
  </div>
</template>
