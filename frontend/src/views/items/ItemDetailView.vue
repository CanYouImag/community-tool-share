<script setup>
import { favoriteItem, getItem, unfavoriteItem } from '@/api/modules/item';
import { useUserStore } from '@/store/user';
import { formatMoney } from '@/utils/format';
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const user = useUserStore();
const item = ref(null);
const loading = ref(true);
const error = ref('');
const favoriteSaving = ref(false);
const favoriteError = ref('');

async function toggleFavorite() {
  if (!item.value || favoriteSaving.value) return;

  if (!user.isLoggedIn) {
    favoriteError.value = '请先登录后再收藏物品';
    return;
  }

  const target = item.value;
  favoriteSaving.value = true;
  favoriteError.value = '';

  try {
    const result = target.favorited
      ? await unfavoriteItem(target.id)
      : await favoriteItem(target.id);

    if (item.value === target) {
      target.favorited = result.favorited;
    }
  } catch (err) {
    if (item.value === target) {
      favoriteError.value = err.message || '收藏操作失败，请重试';
    }
  } finally {
    favoriteSaving.value = false;
  }
}

watch(
  () => route.params.id,
  async (id, _oldId, onCleanup) => {
    let cancelled = false;
    onCleanup(() => {
      cancelled = true;
    });

    loading.value = true;
    error.value = '';
    favoriteError.value = '';
    item.value = null;

    try {
      const data = await getItem(id);
      if (!cancelled) {
        item.value = data;
      }
    } catch (err) {
      if (!cancelled) {
        error.value = err.message || '物品加载失败，请稍后重试';
      }
    } finally {
      if (!cancelled) {
        loading.value = false;
      }
    }
  },
  { immediate: true },
);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>物品详情</h1>
      </div>
      <router-link class="btn" to="/items"> 返回物品广场 </router-link>
    </header>

    <section v-if="loading" class="panel" role="status">正在加载物品…</section>

    <section v-else-if="error" class="panel" role="alert">
      <h2>暂时无法查看物品</h2>
      <p>{{ error }}</p>
    </section>

    <div v-else-if="item" class="two">
      <section class="panel">
        <img class="art large" :src="item.image" :alt="`${item.title}示意图`" />
      </section>

      <section class="panel">
        <h2>{{ item.title }}</h2>
        <p class="muted">{{ item.category }} / {{ item.communityName }}</p>

        <hr class="rule" />

        <p class="price">
          {{ formatMoney(item.dailyPrice) }}
          <small>/ 天</small>
        </p>

        <dl class="line-list">
          <div>
            <dt>押金</dt>
            <dd>{{ formatMoney(item.deposit) }}</dd>
          </div>
          <div>
            <dt>出借人</dt>
            <dd>{{ item.owner }}</dd>
          </div>
          <div>
            <dt>信用分</dt>
            <dd>{{ item.ownerCredit }}</dd>
          </div>
          <div>
            <dt>所属社区</dt>
            <dd>{{ item.communityName }}</dd>
          </div>
        </dl>

        <p v-if="!user.isLoggedIn" role="status">登录后即可收藏此物品。</p>
        <div v-else class="actions">
          <button
            class="btn"
            type="button"
            :disabled="favoriteSaving"
            :aria-pressed="Boolean(item.favorited)"
            @click="toggleFavorite"
          >
            {{ favoriteSaving ? '处理中…' : item.favorited ? '取消收藏' : '收藏' }}
          </button>
        </div>
        <p v-if="favoriteError" role="alert">
          {{ favoriteError }}
        </p>
      </section>
    </div>
  </main>
</template>
