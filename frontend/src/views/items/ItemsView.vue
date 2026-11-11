<script setup>
import { getItems } from '@/api/modules/item';
import EmptyState from '@/components/EmptyState.vue';
import ItemCard from '@/components/ItemCard.vue';
import { useUserStore } from '@/store/user';
import { ITEM_CATEGORY } from '@/utils/constants';
import { onMounted, ref } from 'vue';

const user = useUserStore();
const items = ref([]);
const total = ref(0);
const loading = ref(true);
const error = ref('');
const keyword = ref('');
const category = ref('');
const activeTab = ref('browse');

function switchTab(tab) {
  if (loading.value || activeTab.value === tab) return;

  activeTab.value = tab;
  keyword.value = '';
  category.value = '';
  loadItems();
}

function searchItems() {
  if (loading.value) return;
  loadItems();
}

function resetFilters() {
  if (loading.value) return;
  keyword.value = '';
  category.value = '';
  loadItems();
}

async function loadItems() {
  if (activeTab.value === 'favorites' && !user.isLoggedIn) {
    items.value = [];
    total.value = 0;
    error.value = '';
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = '';

  try {
    const data = await getItems({
      keyword: keyword.value.trim(),
      category: category.value,
      favoritesOnly: activeTab.value === 'favorites',
    });
    items.value = data.records;
    total.value = data.total;
  } catch (err) {
    error.value = err.message || '物品加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(loadItems);
</script>

<template>
  <main class="public-main">
    <div class="page-head">
      <div>
        <h1>物品广场</h1>
        <p>发现邻里好物，让闲置工具流动起来。</p>
      </div>
    </div>

    <div class="view-tabs" role="group" aria-label="物品列表范围">
      <button
        class="btn"
        :class="{ primary: activeTab === 'browse' }"
        type="button"
        :aria-pressed="activeTab === 'browse'"
        :disabled="loading"
        @click="switchTab('browse')"
      >
        浏览物品
      </button>

      <button
        class="btn"
        :class="{ primary: activeTab === 'favorites' }"
        type="button"
        :aria-pressed="activeTab === 'favorites'"
        :disabled="loading"
        @click="switchTab('favorites')"
      >
        我的收藏
      </button>
    </div>

    <section v-if="activeTab === 'favorites' && !user.isLoggedIn" class="panel" role="alert">
      <h2>请先登录</h2>
      <p>登录后即可收藏物品并查看自己的收藏。</p>
      <button class="btn" type="button" @click="switchTab('browse')">返回浏览物品</button>
    </section>

    <form
      v-if="activeTab !== 'favorites' || user.isLoggedIn"
      class="panel"
      @submit.prevent="searchItems"
    >
      <div class="filters">
        <label class="field">
          <span>关键词</span>
          <input
            v-model="keyword"
            type="search"
            placeholder="搜索电钻、帐篷等物品"
            :disabled="loading"
          />
        </label>

        <label class="field">
          <span>分类</span>
          <select v-model="category" :disabled="loading">
            <option value="">全部分类</option>
            <option v-for="name in ITEM_CATEGORY" :key="name" :value="name">
              {{ name }}
            </option>
          </select>
        </label>
      </div>

      <div class="actions">
        <button class="btn primary" type="submit" :disabled="loading">搜索</button>
        <button class="btn" type="button" :disabled="loading" @click="resetFilters">
          清除筛选
        </button>
      </div>
    </form>

    <section v-if="activeTab !== 'favorites' || user.isLoggedIn" class="panel">
      <p v-if="loading" role="status">正在加载物品…</p>

      <div v-else-if="error" role="alert">
        <p>{{ error }}</p>
        <button class="btn" type="button" @click="loadItems">重新加载</button>
      </div>

      <template v-else>
        <div class="panel-title">
          <h2>
            {{ activeTab === 'favorites' ? '我的收藏' : '浏览物品' }}
          </h2>
          <small>共 {{ total }} 件物品</small>
        </div>

        <EmptyState
          v-if="items.length === 0"
          :text="activeTab === 'favorites' ? '没有符合条件的收藏物品' : '没有符合条件的物品'"
        />

        <div v-else class="item-grid">
          <ItemCard v-for="item in items" :key="item.id" :item="item" />
        </div>
      </template>
    </section>
  </main>
</template>
