<script setup>
defineProps({
  community: { type: Object, required: true },
  joinable: { type: Boolean, default: true },
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
      <button type="button" v-if="joinable" class="btn primary" @click="emit('join', community)">
        申请加入
      </button>
    </div>
  </article>
</template>
