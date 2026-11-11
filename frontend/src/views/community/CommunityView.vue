<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import CommunityCard from '@/components/CommunityCard.vue';
import ItemCard from '@/components/ItemCard.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import EmptyState from '@/components/EmptyState.vue';
import Pagination from '@/components/Pagination.vue';
import {
  getCommunities,
  getCommunityHome,
  getMyMemberships,
  joinCommunity,
} from '@/api/modules/community';
import { MEMBER_STATUS } from '@/utils/constants';

// 页内 Tab
const tabs = [
  { id: 'discover', label: '发现社区' },
  { id: 'home', label: '社区主页' },
  { id: 'joined', label: '我的社区' },
];
const activeTab = ref('discover');

// 发现社区
const keyword = ref('');
const allCommunities = ref([]);
const filteredCommunities = computed(() => {
  if (!keyword.value.trim()) return allCommunities.value;
  return allCommunities.value.filter((c) => c.name.includes(keyword.value.trim()));
});

// 社区主页
const currentCommunity = ref(null);
const homeData = reactive({ top: [], latest: [] });
const joinState = ref(''); // '' | 'APPLIED' | 'JOINED'

// 我的社区
const memberships = ref([]);

async function loadCommunities() {
  allCommunities.value = await getCommunities();
}

async function loadMemberships() {
  memberships.value = await getMyMemberships();
}

async function enterCommunity(c) {
  currentCommunity.value = c;
  homeData.top = [];
  homeData.latest = [];
  activeTab.value = 'home';
  const home = await getCommunityHome(c.id);
  currentCommunity.value = home;
  homeData.top = home.top || [];
  homeData.latest = home.latest || [];
  // 判定当前用户在该社区的关系
  const m = memberships.value.find((x) => x.id === c.id);
  joinState.value = m ? (m.status === 'JOINED' ? 'JOINED' : 'APPLIED') : '';
}

// 切换到社区主页：若尚未选择社区，默认加载用户已加入的第一个社区
async function openHomeTab() {
  activeTab.value = 'home';
  if (currentCommunity.value) return;
  const joined = memberships.value.find((m) => m.status === 'JOINED');
  if (joined) {
    const c = allCommunities.value.find((x) => x.id === joined.id);
    if (c) await enterCommunity(c);
  }
}

// 统一 Tab 切换入口：社区主页需先确保已选择社区
function switchTab(id) {
  if (id === 'home') {
    openHomeTab();
  } else {
    activeTab.value = id;
  }
}

async function applyJoin(c) {
  await joinCommunity(c.id);
  joinState.value = 'APPLIED';
  // 同步到我的社区列表
  if (!memberships.value.find((m) => m.id === c.id)) {
    memberships.value.push({
      id: c.id,
      name: c.name,
      status: 'PENDING',
      time: new Date().toISOString().slice(0, 10),
    });
  }
}

function memberBadge(status) {
  return status === 'JOINED' ? MEMBER_STATUS.JOINED : MEMBER_STATUS.PENDING;
}

onMounted(() => {
  loadCommunities();
  loadMemberships();
});
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>社区</h1>
      </div>
    </header>

    <nav class="view-tabs" role="tablist" aria-label="页内功能">
      <a
        v-for="t in tabs"
        :key="t.id"
        class="view-tab"
        :href="`#${t.id}`"
        role="tab"
        :aria-selected="activeTab === t.id"
        @click.prevent="switchTab(t.id)"
      >
        {{ t.label }}
      </a>
    </nav>

    <!-- 发现社区 -->
    <section v-show="activeTab === 'discover'" class="view-panel">
      <h2 class="view-heading">发现社区</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>社区名称</span>
            <input
              v-model="keyword"
              type="text"
              aria-label="社区名称"
              placeholder="输入社区名称搜索"
            />
          </label>
        </div>
      </section>

      <EmptyState
        v-if="!filteredCommunities.length"
        text="没有找到匹配的社区"
        action-text="查看全部社区"
        @action="keyword = ''"
      />
      <div v-else class="three">
        <CommunityCard
          v-for="c in filteredCommunities"
          :key="c.id"
          :community="c"
          @enter="enterCommunity"
          @join="applyJoin"
        />
      </div>
    </section>

    <!-- 社区主页 -->
    <section v-show="activeTab === 'home'" class="view-panel">
      <h2 class="view-heading">社区主页</h2>
      <template v-if="currentCommunity">
        <section class="panel">
          <div class="row between">
            <div>
              <h2>{{ currentCommunity.intro || currentCommunity.name }}</h2>
              <p class="muted">
                社区管理员：{{ currentCommunity.admin }} / 成员 {{ currentCommunity.members }} 人 /
                在租物品 {{ currentCommunity.onRent }} 件
              </p>
            </div>
            <a v-if="!joinState" class="btn primary" @click="applyJoin(currentCommunity)"
              >申请加入</a
            >
            <span v-else-if="joinState === 'APPLIED'" class="badge warn">待审核</span>
            <span v-else class="badge">已加入</span>
          </div>
        </section>

        <div class="info">社区公告：{{ currentCommunity.notice }}</div>

        <section class="panel">
          <div class="panel-title">
            <h2>置顶推荐</h2>
            <span class="badge">本社区最多置顶 3 件</span>
          </div>
          <EmptyState v-if="!homeData.top.length" text="暂无置顶物品" />
          <div v-else class="item-grid">
            <ItemCard v-for="it in homeData.top" :key="it.id" :item="it" />
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <h2>最新上架</h2>
          </div>
          <EmptyState v-if="!homeData.latest.length" text="暂无上架物品" />
          <div v-else class="item-grid">
            <ItemCard v-for="it in homeData.latest" :key="it.id" :item="it" />
          </div>
        </section>
      </template>
      <EmptyState
        v-else
        text="请先选择一个社区"
        action-text="去发现社区"
        @action="activeTab = 'discover'"
      />
    </section>

    <!-- 我的社区 -->
    <section v-show="activeTab === 'joined'" class="view-panel">
      <h2 class="view-heading">我的社区</h2>
      <section class="panel">
        <div class="panel-title">
          <h2>成员关系</h2>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">社区</th>
                <th scope="col">状态</th>
                <th scope="col">加入／申请时间</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in memberships" :key="m.id">
                <td>{{ m.name }}</td>
                <td>
                  <StatusBadge :status="m.status === 'JOINED' ? 'JOINED' : 'MEMBER_PENDING'" />
                </td>
                <td>{{ m.time }}</td>
                <td>
                  <div class="actions">
                    <a
                      v-if="m.status === 'JOINED'"
                      class=""
                      @click="enterCommunity(allCommunities.find((c) => c.id === m.id))"
                      >社区主页</a
                    >
                    <template v-if="m.status === 'JOINED'">
                      <a class="">退出社区</a>
                    </template>
                    <span v-else>等待审核</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="memberships.length" />
      </section>
    </section>
  </main>
</template>
