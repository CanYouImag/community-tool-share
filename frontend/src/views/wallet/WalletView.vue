<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getWallet, getFlows, getDeposits } from '@/api/modules/wallet'
import { FLOW_TYPE, FLOW_TYPE_FILTER } from '@/utils/constants'
import { formatAmount } from '@/utils/format'

const router = useRouter()

const wallet = ref(null)
const flows = ref([])
const deposits = ref([])
const flowType = ref('')
const flowTotal = ref(0)

const tabs = [
  { id: 'flows', label: '资金流水' },
  { id: 'deposits', label: '押金明细' }
]
const activeTab = ref('flows')

async function load() {
  wallet.value = await getWallet()
  deposits.value = await getDeposits()
  await loadFlows()
}

async function loadFlows() {
  const res = await getFlows({ type: flowType.value })
  flows.value = res.list
  flowTotal.value = res.total
}

// 流水金额显示（带符号 + 类型前缀）
function flowText(f) {
  const type = FLOW_TYPE[f.type] || {}
  const sign = type.sign || ''
  return `${sign} ¥${formatAmount(f.amount)}`
}

function flowLabel(f) {
  return FLOW_TYPE[f.type]?.label || f.type
}

function depositStatus(s) {
  return s === 'FREEZE' ? '冻结中' : '已解冻'
}

onMounted(load)
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>钱包</h1>
      </div>
    </header>

    <div class="stats">
      <div class="stat">
        <small>可用余额</small>
        <div class="value">{{ formatAmount(wallet?.balance) }}</div>
      </div>
      <div class="stat">
        <small>冻结金额</small>
        <div class="value">{{ formatAmount(wallet?.frozen) }}</div>
      </div>
      <div class="stat">
        <small>累计收入</small>
        <div class="value">{{ formatAmount(wallet?.totalIncome) }}</div>
      </div>
      <div class="stat">
        <small>本次租金支出</small>
        <div class="value">{{ formatAmount(wallet?.recentRentSpent) }}</div>
      </div>
    </div>

    <nav class="view-tabs" role="tablist" aria-label="页内功能">
      <a
        v-for="t in tabs"
        :key="t.id"
        class="view-tab"
        :href="`#${t.id}`"
        role="tab"
        :aria-selected="activeTab === t.id"
        @click.prevent="activeTab = t.id"
      >
        {{ t.label }}
      </a>
    </nav>

    <!-- 资金流水 -->
    <section v-show="activeTab === 'flows'" class="view-panel">
      <h2 class="view-heading">资金流水</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>流水类型</span>
            <select v-model="flowType" aria-label="流水类型" @change="loadFlows">
              <option v-for="o in FLOW_TYPE_FILTER" :key="o.value" :value="o.value">
                {{ o.label }}
              </option>
            </select>
          </label>
        </div>

        <EmptyState v-if="!flows.length" text="暂无资金流水" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">时间</th>
                <th scope="col">类型</th>
                <th scope="col">金额</th>
                <th scope="col">关联订单 / 备注</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in flows" :key="f.id">
                <td>{{ f.time }}</td>
                <td>{{ flowLabel(f) }}</td>
                <td>{{ flowText(f) }}</td>
                <td>{{ f.orderId || f.remark }}</td>
                <td>
                  <a
                    v-if="f.orderId"
                    class=""
                    @click="router.push({ name: 'order-detail', params: { id: f.orderId } })"
                    >订单详情</a
                  >
                  <span v-else>—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="flowTotal" />
      </section>
    </section>

    <!-- 押金明细 -->
    <section v-show="activeTab === 'deposits'" class="view-panel">
      <h2 class="view-heading">押金明细</h2>
      <section class="panel">
        <EmptyState v-if="!deposits.length" text="暂无押金记录" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">关联订单</th>
                <th scope="col">物品</th>
                <th scope="col">押金金额</th>
                <th scope="col">状态</th>
                <th scope="col">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in deposits" :key="d.orderId + d.time">
                <td>{{ d.orderId }}</td>
                <td>{{ d.itemTitle }}</td>
                <td>¥{{ formatAmount(d.amount) }}</td>
                <td>
                  <StatusBadge
                    :label="depositStatus(d.status)"
                    :cls="d.status === 'FREEZE' ? 'warn' : ''"
                  />
                </td>
                <td>{{ d.time }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="deposits.length" />
      </section>
    </section>
  </main>
</template>
