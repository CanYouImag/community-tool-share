<script setup>
import { formatMoney } from '@/utils/format';

defineProps({
  item: { type: Object, required: true },
  listQuery: { type: Object, default: undefined },
});
</script>

<template>
  <article class="item-card">
    <router-link :to="{ name: 'item-detail', params: { id: item.id }, query: listQuery }">
      <img class="art" :src="item.images?.[0]" :alt="`${item.title}示意图`" />
    </router-link>
    <div class="body">
      <small>{{ item.categoryName }} / {{ item.communityName }}</small>
      <h3>
        <router-link :to="{ name: 'item-detail', params: { id: item.id }, query: listQuery }">{{
          item.title
        }}</router-link>
      </h3>
      <p class="muted">浏览 {{ item.viewCount ?? 0 }} 次</p>
      <div class="bottom">
        <span class="price">
          {{ formatMoney(item.rentPerDay) }}
          <small> / 天</small>
        </span>
        <small>{{ item.ownerNickname }}</small>
      </div>
    </div>
  </article>
</template>
