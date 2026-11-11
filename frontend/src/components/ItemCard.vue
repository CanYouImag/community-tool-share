<script setup>
import { formatRating } from '@/utils/format';

defineProps({
  item: { type: Object, required: true },
  listQuery: { type: Object, default: undefined },
});
</script>

<template>
  <article class="item-card">
    <router-link :to="{ name: 'item-detail', params: { id: item.id }, query: listQuery }">
      <img class="art" :src="item.image" :alt="`${item.title}示意图`" />
    </router-link>
    <div class="body">
      <small>{{ item.category }} / {{ item.communityName }}</small>
      <h3>
        <router-link :to="{ name: 'item-detail', params: { id: item.id }, query: listQuery }">{{
          item.title
        }}</router-link>
      </h3>
      <p class="muted">物品评分：{{ formatRating(item.rating) }}</p>
      <div class="bottom">
        <span class="price">
          ¥{{ item.dailyPrice }}
          <small> / 天</small>
        </span>
        <small>{{ item.owner }}</small>
      </div>
    </div>
  </article>
</template>
