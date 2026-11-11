<script setup>
import { ref, reactive, onMounted } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getDisputes,
  getDisputeDetail,
  mediateDispute,
  resolveDispute,
  escalateDispute
} from '@/api/modules/admin'
import { formatAmount, formatDateTime } from '@/utils/format'

const STATUS_OPTIONS = [
  { value: 'MEDIATING', label: '调解中' },
  { value: 'PENDING_ARBITRATION', label: '待仲裁' },
  { value: 'RESOLVED', label: '已调解' },
  { value: 'ARBITRATED', label: '已裁决' }
]

const status = ref('MEDIATING')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const detail = ref(null)
const mediateText = ref('')
const resolveText = ref('')
const escalateText = ref('')

async function load() {
  const data = await getDisputes({ status: status.value || undefined, page: page.value, size: size.value })
  list.value = data.records || []
  total.value = data.total || 0
}

function changePage(p) {
  page.value = p
  load()
}

async function select(d) {
  detail.value = await getDisputeDetail(d.id)
}

async function sendMessage() {
  if (!mediateText.value.trim() || !detail.value) return
  await mediateDispute(detail.value.id, { content: mediateText.value })
  mediateText.value = ''
  await select(detail.value)
}

async function doResolve() {
  if (!resolveText.value.trim() || !detail.value) return
  await resolveDispute(detail.value.id, { conclusion: resolveText.value })
  resolveText.value = ''
  detail.value = null
  load()
}

async function doEscalate() {
  if (!escalateText.value.trim() || !detail.value) return
  await escalateDispute(detail.value.id, { note: escalateText.value })
  escalateText.value = ''
  detail.value = null
  load()
}

onMounted(load)
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>调解工作台</h1>
      </div>
    </header>

    <section class="view-panel">
      <h2 class="view-heading">纠纷队列</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>纠纷状态</span>
            <select v-model="status" aria-label="纠纷状态" @change="page = 1; load()">
              <option value="">全部</option>
              <option v-for="o in STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>
        </div>

        <EmptyState v-if="!list.length" text="暂无纠纷" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">编号 / 订单</th>
                <th scope="col">社区</th>
                <th scope="col">发起人</th>
                <th scope="col">涉及押金</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in list" :key="d.id">
                <td>{{ d.id }}<small>订单 #{{ d.orderId }}</small></td>
                <td>{{ d.communityName }}</td>
                <td>{{ d.initiatorName }}</td>
                <td>¥{{ formatAmount(d.deposit) }}</td>
                <td><StatusBadge :status="d.status" /></td>
                <td><a @click="select(d)">介入调解</a></td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="total" :page="page" :page-size="size" @change="changePage" />
      </section>
    </section>

    <!-- 详情 -->
    <section v-if="detail" class="view-panel">
      <h2 class="view-heading">纠纷详情 #{{ detail.id }}</h2>
      <div class="two">
        <div>
          <section class="panel">
            <div class="row between">
              <div>
                <small>订单 #{{ detail.orderId }} / {{ detail.communityName }}</small>
                <h2>押金争议</h2>
              </div>
              <StatusBadge :status="detail.status" />
            </div>
            <dl class="line-list">
              <div><dt>发起人</dt><dd>{{ detail.initiatorName }}</dd></div>
              <div><dt>租客</dt><dd>{{ detail.renterName }}</dd></div>
              <div><dt>出租者</dt><dd>{{ detail.ownerName }}</dd></div>
              <div><dt>纠纷理由</dt><dd>{{ detail.reason }}</dd></div>
              <div><dt>押金 / 租金</dt><dd>¥{{ formatAmount(detail.deposit) }} / ¥{{ formatAmount(detail.rent) }}</dd></div>
              <div v-if="detail.mediationConclusion"><dt>调解结论</dt><dd>{{ detail.mediationConclusion }}</dd></div>
              <div v-if="detail.escalateNote"><dt>上报说明</dt><dd>{{ detail.escalateNote }}</dd></div>
            </dl>
            <div v-if="detail.evidences?.length" class="thumbs">
              <img v-for="e in detail.evidences" :key="e.id" class="art small" :src="e.imageUrl" :alt="e.evidenceType" />
            </div>
          </section>
        </div>
        <div>
          <section class="panel">
            <div class="panel-title">
              <h2>处理记录</h2>
            </div>
            <div v-if="detail.messages?.length" class="timeline">
              <p v-for="m in detail.messages" :key="m.id">
                {{ m.senderName }}（{{ m.senderRole }}）
                <small>{{ formatDateTime(m.createdAt) }}</small>
                <span>{{ m.content }}</span>
              </p>
            </div>
            <p v-else class="muted">暂无留言</p>
          </section>

          <section v-if="detail.status === 'MEDIATING'" class="panel">
            <div class="panel-title">
              <h2>调解操作</h2>
            </div>
            <label class="field">
              <span>调解留言</span>
              <textarea v-model="mediateText" aria-label="调解留言"></textarea>
            </label>
            <div class="actions">
              <a class="btn primary" @click="sendMessage">发送留言</a>
            </div>
            <hr class="rule" />
            <label class="field">
              <span>调解结论（达成一致时填写）</span>
              <textarea v-model="resolveText" aria-label="调解结论"></textarea>
            </label>
            <div class="actions">
              <a class="btn" @click="doResolve">标记调解成功</a>
            </div>
            <hr class="rule" />
            <label class="field">
              <span>上报仲裁说明（调解不成时填写）</span>
              <textarea v-model="escalateText" aria-label="上报仲裁说明"></textarea>
            </label>
            <div class="actions">
              <a class="btn" @click="doEscalate">上报平台仲裁</a>
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>
