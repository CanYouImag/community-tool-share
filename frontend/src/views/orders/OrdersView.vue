<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getOrders } from '@/api/modules/order'
import { ORDER_STATUS_FILTER } from '@/utils/constants'

const router = useRouter()

const activeTab = ref('borrow') // borrow | lend
const keyword = ref('')
const status = ref('')
const list = ref([])
const total = ref(0)

async function load() {
  const role = activeTab.value
  const res = await getOrders({
    role,
    keyword: keyword.value,
    status: status.value
  })
  list.value = res.list
  total.value = res.total
}

function switchTab(tab) {
  activeTab.value = tab
  load()
}

function goDetail(order, action) {
  // 不同状态跳转对应订单阶段锚点（Mock 阶段统一到详情页）
  const hash = {
    PENDING: 'pending',
    TO_PICKUP: 'pickup',
    OVERDUE: 'renting',
    COMPLETED: 'complete',
    RENTING: 'renting'
  }[order.status] || 'pending'
  router.push({ name: 'order-detail', params: { id: order.id }, hash: `#${hash}` })
}

// 操作文案：区分租入/租出视角
function actionText(order) {
  const role = activeTab.value
  const map = {
    PENDING: role === 'borrow' ? '订单详情' : '处理订单',
    TO_PICKUP: role === 'borrow' ? '查看取物码' : '核销取物',
    OVERDUE: '查看详情',
    RENTING: '查看详情',
    COMPLETED: '评价'
  }
  return map[order.status] || '订单详情'
}

onMounted(load)
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>我的订单</h1>
      </div>
    </header>

    <nav class="view-tabs" role="tablist" aria-label="页内功能">
      <a
        class="view-tab"
        href="#borrow"
        role="tab"
        :aria-selected="activeTab === 'borrow'"
        @click.prevent="switchTab('borrow')"
        >我租入的</a
      >
      <a
        class="view-tab"
        href="#lend"
        role="tab"
        :aria-selected="activeTab === 'lend'"
        @click.prevent="switchTab('lend')"
        >我租出的</a
      >
    </nav>

    <section class="view-panel">
      <h2 class="view-heading">{{ activeTab === 'borrow' ? '我租入的' : '我租出的' }}</h2>
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
            <a class="btn primary" @click="load">搜索</a>
            <a class="btn" @click="keyword = ''; status = ''; load()">清除筛选</a>
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
                  <a class="" @click="goDetail(o)">{{ actionText(o) }}</a>
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
