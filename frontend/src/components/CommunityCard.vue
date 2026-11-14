<script setup>
defineProps({
  community: { type: Object, required: true },
  joinable: { type: Boolean, default: true },
  joinState: { type: String, default: '' },
});

const emit = defineEmits(['enter', 'join']);
</script>

<template>
  <article class="community-card">
    <div class="cover">
      <h2>{{ community.name }}</h2>
    </div>
    <div class="body">
      <dl class="line-list">
        <div>
          <dt>成员数</dt>
          <dd>{{ community.members }} 人</dd>
        </div>
        <div>
          <dt>在租物品</dt>
          <dd>{{ community.onRent }} 件</dd>
        </div>
      </dl>
      <button type="button" class="btn" @click="emit('enter', community)">进入社区</button>
      <button
        v-if="joinable && !joinState"
        type="button"
        class="btn primary"
        @click="emit('join', community)"
      >
        {{ community.autoApprove ? '加入社区' : '申请加入' }}
      </button>
      <span v-else-if="joinState === 'APPLIED'" class="badge warn">待审核</span>
      <span v-else-if="joinState === 'JOINED'" class="badge">已加入</span>
      <small v-if="community.autoApprove" class="muted">该社区自动通过入社申请</small>
    </div>
  </article>
</template>
