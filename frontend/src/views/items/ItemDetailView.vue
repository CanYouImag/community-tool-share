<script setup>
import { favoriteItem, getItem, unfavoriteItem } from '@/api/modules/item';
import { useUserStore } from '@/store/user';
import { formatMoney } from '@/utils/format';
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const user = useUserStore();
const item = ref(null);
const loading = ref(true);
const error = ref('');
const favoriteSaving = ref(false);
const favoriteError = ref('');
const selectedImageIndex = ref(0);
const imageFailed = ref(false);

const images = computed(() => {
  const pictures = Array.isArray(item.value?.images)
    ? item.value.images.filter((url) => typeof url === 'string' && url.trim())
    : [];

  if (pictures.length > 0) return pictures;
  return item.value?.image ? [item.value.image] : [];
});
const selectedImage = computed(() => images.value[selectedImageIndex.value] || images.value[0]);

watch(selectedImage, () => {
  imageFailed.value = false;
});

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
    selectedImageIndex.value = 0;
    imageFailed.value = false;

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
        <img
          v-if="selectedImage && !imageFailed"
          :key="selectedImage"
          class="art large"
          :src="selectedImage"
          :alt="`${item.title}，第 ${selectedImageIndex + 1} 张图片`"
          @error="imageFailed = true"
        />
        <div v-else class="image-placeholder" role="status">
          <p>{{ imageFailed ? '图片加载失败' : '暂无物品图片' }}</p>
          <button v-if="imageFailed" class="btn" type="button" @click="imageFailed = false">
            重新加载图片
          </button>
        </div>

        <template v-if="images.length > 1">
          <p class="help" aria-live="polite">
            第 {{ selectedImageIndex + 1 }} / {{ images.length }} 张图片
          </p>
          <div class="gallery-thumbnails" role="group" aria-label="选择物品图片">
            <button
              v-for="(url, index) in images"
              :key="index"
              class="gallery-thumbnail"
              type="button"
              :aria-label="`查看第 ${index + 1} 张图片`"
              :aria-pressed="selectedImageIndex === index"
              @click="selectedImageIndex = index"
            >
              <img :src="url" alt="" loading="lazy" />
            </button>
          </div>
        </template>
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

        <section class="item-copy" aria-labelledby="item-condition-heading">
          <h3 id="item-condition-heading">成色说明</h3>
          <p>{{ item.condition?.trim() || '暂无成色说明' }}</p>
        </section>

        <section class="item-copy" aria-labelledby="item-description-heading">
          <h3 id="item-description-heading">物品描述</h3>
          <p>{{ item.description?.trim() || '暂无物品描述' }}</p>
        </section>

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

<style scoped>
.gallery-thumbnails {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(64px, 1fr));
  gap: 8px;
}

.gallery-thumbnail {
  min-width: 0;
  padding: 4px;
  border: 2px solid var(--color-rule);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  cursor: pointer;
}

.gallery-thumbnail[aria-pressed='true'] {
  border-color: var(--color-accent);
  background: var(--color-soft);
}

.gallery-thumbnail img {
  display: block;
  width: 100%;
  height: 64px;
  object-fit: contain;
}

.image-placeholder {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 12px;
  min-height: 260px;
  border-radius: var(--radius-sm);
  background: var(--color-art);
  color: var(--color-muted);
}

.item-copy {
  margin-block: 20px;
}

.item-copy p {
  margin-top: 8px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
