<script setup>
import { ref, onMounted } from 'vue';

import { getOrders } from '@/api/modules/order';
import EmptyState from '@/components/EmptyState.vue';
import Pagination from '@/components/Pagination.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import ViewTabs from '@/components/ViewTabs.vue';
import { ORDER_STATUS_FILTER } from '@/utils/constants';

const tabs = [
  { id: 'borrow', label: '我租入的' },
  { id: 'lend', label: '我租出的' },
];

const activeTab = ref('borrow'); // borrow | lend
const keyword = ref('');
const status = ref('');
const list = ref([]);
const total = ref(0);

async function load() {
  const role = activeTab.value;
  const res = await getOrders({
    role,
    keyword: keyword.value,
    status: status.value,
  });
  list.value = res.list;
  total.value = res.total;
}

function resetFilters() {
  keyword.value = '';
  status.value = '';
  load();
}

function switchTab(tab) {
  activeTab.value = tab;
  load();
}

function orderLocation(order) {
  // 不同状态跳转对应订单阶段锚点（Mock 阶段统一到详情页）
  const hash =
    {
      PENDING_PAYMENT: 'pending',
      PENDING_ACCEPT: 'checkout',
      PENDING_PICKUP: 'pickup',
      IN_USE: 'renting',
      PENDING_RETURN: 'return',
      PENDING_INSPECT: 'inspect',
      OVERDUE: 'renting',
      COMPLETED: 'complete',
      DISPUTED: 'renting',
      CLOSED: 'complete',
    }[order.status] || 'pending';
  return { name: 'order-detail', params: { id: order.id }, hash: `#${hash}` };
}

// 操作文案：区分租入/租出视角
function actionText(order) {
  const role = activeTab.value;
  const map = {
    PENDING_PAYMENT: role === 'borrow' ? '订单详情' : '处理订单',
    PENDING_ACCEPT: role === 'borrow' ? '确认支付' : '等待租客支付',
    PENDING_PICKUP: role === 'borrow' ? '查看取物码' : '核销取物',
    IN_USE: '查看详情',
    PENDING_RETURN: role === 'borrow' ? '发起归还' : '等待归还',
    PENDING_INSPECT: role === 'borrow' ? '等待验收' : '验收物品',
    OVERDUE: '查看详情',
    COMPLETED: '评价',
    DISPUTED: '查看纠纷',
    CLOSED: '查看详情',
  };
  return map[order.status] || '订单详情';
}

onMounted(load);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>我的订单</h1>
      <router-link class="btn primary" :to="{ name: 'dispute' }">发起纠纷 / 举报</router-link>
    </header>

    <ViewTabs
      :model-value="activeTab"
      :tabs="tabs"
      label="订单类型"
      @update:model-value="switchTab"
    />

    <section class="view-panel">
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>订单号 / 物品</span>
            <input v-model="keyword" type="text" aria-label="订单号 / 物品" @keyup.enter="load" />
          </label>
          <label class="field">
            <span>订单状态</span>
            <select v-model="status" aria-label="订单状态" @change="load">
              <option v-for="o in ORDER_STATUS_FILTER" :key="o.value" :value="o.value">
                {{ o.label }}
              </option>
            </select>
          </label>
          <div class="actions">
            <button type="button" class="btn primary" @click="load">搜索</button>
            <button type="button" class="btn" @click="resetFilters">清除筛选</button>
          </div>
        </div>

        <EmptyState v-if="!list.length" text="暂无符合条件的订单" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">订单 / 物品</th>
                <th scope="col">{{ activeTab === 'borrow' ? '出租者' : '租客' }}</th>
                <th scope="col">租期</th>
                <th scope="col">租金 / 押金</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in list" :key="o.id">
                <td>
                  {{ o.id }}
                  <small>{{ o.itemTitle }}</small>
                </td>
                <td>{{ activeTab === 'borrow' ? o.lender : o.borrower }}</td>
                <td>{{ o.startDate.slice(5) }} 至 {{ o.endDate.slice(5) }}</td>
                <td>¥{{ o.rent }} / ¥{{ o.deposit }}</td>
                <td><StatusBadge :status="o.status" /></td>
                <td>
                  <router-link :to="orderLocation(o)">{{ actionText(o) }}</router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="total" />
      </section>
    </section>
  </main>
</template>
