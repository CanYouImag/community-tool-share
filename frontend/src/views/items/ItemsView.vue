<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getCommunities } from '@/api/modules/community';
import { getCategories, getFavorites, getItems } from '@/api/modules/item';
import EmptyState from '@/components/EmptyState.vue';
import ItemCard from '@/components/ItemCard.vue';
import Pagination from '@/components/Pagination.vue';
import ViewTabs from '@/components/ViewTabs.vue';
import { useUserStore } from '@/store/user';
import { ITEM_SORT_OPTIONS, parsePriceRange } from '@/utils/itemFilters';
import { itemListQuery, readItemListQuery } from '@/utils/itemListRoute';

const tabs = [
  { id: 'browse', label: '浏览物品' },
  { id: 'favorites', label: '我的收藏' },
];

const user = useUserStore();
const route = useRoute();
const router = useRouter();
const items = ref([]);
const total = ref(0);
const loading = ref(true);
const error = ref('');
const keyword = ref('');
const categoryId = ref('');
const minPrice = ref('');
const maxPrice = ref('');
const filterError = ref('');
const sort = ref('newest');
const activeTab = ref('browse');
const communities = ref([]);
const categories = ref([]);
const communityLoading = ref(false);
const communityError = ref('');
const communitySelection = ref('all');
const page = ref(1);
const pageSize = ref(6);
const appliedQuery = ref(null);
const lastRequest = ref(null);
const appliedLocationQuery = computed(() =>
  appliedQuery.value ? itemListQuery(appliedQuery.value, page.value) : {},
);
let requestVersion = 0;
let successfulQueryKey = '';

function restoreFields(query) {
  keyword.value = query.keyword;
  categoryId.value = query.categoryId;
  minPrice.value = query.minPrice ?? '';
  maxPrice.value = query.maxPrice ?? '';
  sort.value = query.sort;
  activeTab.value = query.favoritesOnly ? 'favorites' : 'browse';
  communitySelection.value = query.communityId || 'all';
  pageSize.value = query.size;
  filterError.value = '';
}

async function navigateToQuery(query, targetPage) {
  const destination = { name: 'items', query: itemListQuery(query, targetPage) };
  try {
    if (router.resolve(destination).fullPath === route.fullPath) {
      restoreFields(query);
      await loadItems(query, targetPage);
    } else {
      await router.push(destination);
    }
  } catch (err) {
    error.value = err.message || '页面跳转失败，请重试';
  }
}

async function normalizeLocation(query, currentPage) {
  const destination = { name: 'items', query: itemListQuery(query, currentPage) };
  if (router.resolve(destination).fullPath !== route.fullPath) {
    await router.replace(destination);
  }
}

async function loadCommunities() {
  if (communityLoading.value) return;
  communityLoading.value = true;
  communityError.value = '';
  try {
    communities.value = await getCommunities();
  } catch (err) {
    communityError.value = err.message || '社区列表加载失败';
  } finally {
    communityLoading.value = false;
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategories();
  } catch {
    categories.value = [];
  }
}

function buildQuery() {
  return {
    keyword: keyword.value.trim(),
    categoryId: categoryId.value || undefined,
    favoritesOnly: activeTab.value === 'favorites',
    sort: sort.value,
    communityId: communitySelection.value === 'all' ? undefined : communitySelection.value,
    size: pageSize.value,
    ...parsePriceRange(minPrice.value, maxPrice.value),
  };
}

function clearFilters() {
  keyword.value = '';
  categoryId.value = '';
  minPrice.value = '';
  maxPrice.value = '';
  sort.value = 'newest';
  filterError.value = '';
  communitySelection.value = 'all';
}

function switchTab(tab) {
  if (loading.value || activeTab.value === tab) return;

  activeTab.value = tab;
  clearFilters();
  searchItems();
}

function searchItems() {
  if (loading.value) return;
  filterError.value = '';
  try {
    navigateToQuery(buildQuery(), 1);
  } catch (err) {
    filterError.value = err.message;
  }
}

function resetFilters() {
  if (loading.value) return;
  clearFilters();
  searchItems();
}

function changePage(nextPage) {
  if (loading.value || !appliedQuery.value) return;
  // 翻页使用已提交的条件，不把尚未点击搜索的输入混入请求。
  navigateToQuery(appliedQuery.value, nextPage);
}

function reloadItems() {
  if (loading.value || !lastRequest.value) return;
  loadItems(lastRequest.value.query, lastRequest.value.page);
}

async function loadItems(query, targetPage = 1) {
  const version = ++requestVersion;
  if (query.favoritesOnly && !user.isLoggedIn) {
    items.value = [];
    total.value = 0;
    page.value = 1;
    appliedQuery.value = null;
    lastRequest.value = null;
    error.value = '';
    loading.value = false;
    successfulQueryKey = JSON.stringify(itemListQuery(query, 1));
    await normalizeLocation(query, 1);
    return;
  }

  loading.value = true;
  error.value = '';
  lastRequest.value = { query, page: targetPage };

  try {
    const data = query.favoritesOnly
      ? await getFavorites({ page: targetPage, size: query.size })
      : await getItems({ ...query, page: targetPage });
    if (version !== requestVersion || route.name !== 'items') return;
    items.value = data.records;
    total.value = data.total;
    page.value = data.page;
    appliedQuery.value = query;
    successfulQueryKey = JSON.stringify(itemListQuery(query, data.page));
    await normalizeLocation(query, data.page);
  } catch (err) {
    if (version === requestVersion) {
      successfulQueryKey = '';
      error.value = err.message || '物品加载失败，请稍后重试';
    }
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}

watch(
  () => route.fullPath,
  () => {
    if (route.name !== 'items') return;
    const restored = readItemListQuery(route.query, user.isLoggedIn);
    restoreFields(restored.query);
    const key = JSON.stringify(itemListQuery(restored.query, restored.page));
    // 回到已有结果，或服务端修正网址时，取消旧请求并复用当前数据。
    if (key === successfulQueryKey && !error.value) {
      requestVersion++;
      loading.value = false;
      lastRequest.value = { query: restored.query, page: restored.page };
      return;
    }
    loadItems(restored.query, restored.page);
  },
  { immediate: true },
);

onMounted(() => {
  loadCommunities();
  loadCategories();
});
onBeforeUnmount(() => {
  requestVersion++;
});
</script>

<template>
  <main class="public-main">
    <header class="page-head"><h1>物品广场</h1></header>

    <ViewTabs
      :model-value="activeTab"
      :tabs="tabs"
      :disabled="loading"
      label="物品列表范围"
      @update:model-value="switchTab"
    />

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
          <span>社区范围</span>
          <select
            v-model="communitySelection"
            :disabled="loading || communityLoading"
            @change="searchItems"
          >
            <option value="all">全部社区</option>
            <option v-for="community in communities" :key="community.id" :value="String(community.id)">
              {{ community.name }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>关键词</span>
          <input
            v-model="keyword"
            type="search"
            placeholder="搜索物品标题或描述"
            :disabled="loading"
          />
        </label>

        <label class="field">
          <span>分类</span>
          <select v-model="categoryId" :disabled="loading">
            <option value="">全部分类</option>
            <option v-for="c in categories" :key="c.id" :value="String(c.id)">
              {{ c.name }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>最低日租金（元）</span>
          <input
            v-model="minPrice"
            type="number"
            min="0"
            step="0.01"
            placeholder="不限"
            :disabled="loading"
            :aria-invalid="Boolean(filterError)"
            :aria-describedby="filterError ? 'price-filter-error' : undefined"
          />
        </label>

        <label class="field">
          <span>最高日租金（元）</span>
          <input
            v-model="maxPrice"
            type="number"
            min="0"
            step="0.01"
            placeholder="不限"
            :disabled="loading"
            :aria-invalid="Boolean(filterError)"
            :aria-describedby="filterError ? 'price-filter-error' : undefined"
          />
        </label>
      </div>

      <label class="field">
        <span>排序方式</span>
        <select v-model="sort" :disabled="loading" @change="searchItems">
          <option v-for="option in ITEM_SORT_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label class="field">
        <span>每页数量</span>
        <select v-model.number="pageSize" :disabled="loading" @change="searchItems">
          <option :value="3">3 件</option>
          <option :value="6">6 件</option>
          <option :value="12">12 件</option>
        </select>
      </label>

      <div v-if="communityError" role="alert">
        <p>{{ communityError }}，仍可使用默认范围或全部社区浏览。</p>
        <button class="btn" type="button" :disabled="communityLoading" @click="loadCommunities">
          重新加载社区
        </button>
      </div>

      <p v-if="filterError" id="price-filter-error" role="alert">{{ filterError }}</p>

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
        <button class="btn" type="button" @click="reloadItems">重新加载</button>
      </div>

      <template v-else>
        <EmptyState
          v-if="items.length === 0"
          :text="activeTab === 'favorites' ? '没有符合条件的收藏物品' : '没有符合条件的物品'"
        />

        <div v-else class="item-grid">
          <ItemCard
            v-for="item in items"
            :key="item.id"
            :item="item"
            :list-query="appliedLocationQuery"
          />
        </div>

        <Pagination
          v-if="total > 0"
          :total="total"
          :page="page"
          :page-size="appliedQuery.size"
          :disabled="loading"
          @change="changePage"
        />
      </template>
    </section>
  </main>
</template>
