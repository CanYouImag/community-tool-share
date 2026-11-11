<script setup>
import { ref, reactive, onMounted } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import { getReports, handleReport } from '@/api/modules/admin'
import { formatDateTime } from '@/utils/format'

const REPORT_TARGET = { ITEM: '物品', USER: '用户', REVIEW: '评价' }
const REPORT_TYPE = {
  FALSE_INFO: '信息不实',
  ILLEGAL_CONTENT: '违规内容',
  MALICIOUS_REVIEW: '恶意评价',
  OTHER: '其他'
}
const ACTIONS = [
  { value: 'REJECT', label: '驳回举报' },
  { value: 'ITEM_DOWN', label: '下架物品' },
  { value: 'WARN', label: '警告当事人' },
  { value: 'BAN', label: '移交封禁' }
]

const status = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const handling = reactive({ id: null, action: '', note: '' })

async function load() {
  const data = await getReports({
    status: status.value || undefined,
    page: page.value,
    size: size.value
  })
  list.value = data.records || []
  total.value = data.total || 0
}

function changePage(p) {
  page.value = p
  load()
}

function search() {
  page.value = 1
  load()
}

function begin(r, action) {
  handling.id = r.id
  handling.action = action
  handling.note = ''
}

async function confirm() {
  if (!handling.id) return
  await handleReport(handling.id, { action: handling.action, note: handling.note })
  handling.id = null
  handling.action = ''
  handling.note = ''
  load()
}

onMounted(load)
</script>

<template>
  <section class="panel">
    <div class="filters">
      <label class="field">
        <span>状态</span>
        <select v-model="status" aria-label="举报状态">
          <option value="">全部</option>
          <option value="PENDING">待处理</option>
          <option value="PROCESSED">已处理</option>
        </select>
      </label>
      <a class="btn" @click="search">查询</a>
    </div>

    <div v-if="handling.id" class="info">
      <div class="field">
        <span>处理意见（可填）</span>
        <input v-model="handling.note" type="text" placeholder="填写处理说明" />
      </div>
      <div class="actions">
        <a class="btn primary" @click="confirm">确认处理</a>
        <a class="btn" @click="handling.id = null">取消</a>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th scope="col">举报人</th>
            <th scope="col">对象</th>
            <th scope="col">类型</th>
            <th scope="col">说明</th>
            <th scope="col">状态</th>
            <th scope="col">时间</th>
            <th scope="col">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>{{ r.reporterName }}</td>
            <td>{{ REPORT_TARGET[r.targetType] || r.targetType }}<template v-if="r.targetId"> #{{ r.targetId }}</template></td>
            <td>{{ REPORT_TYPE[r.reasonType] || r.reasonType }}</td>
            <td>{{ r.description }}</td>
            <td>
              <StatusBadge
                :status="r.status"
                :label="r.status === 'PENDING' ? '待处理' : '已处理'"
                :cls="r.status === 'PENDING' ? 'warn' : ''"
              />
            </td>
            <td>{{ formatDateTime(r.createdAt) }}</td>
            <td>
              <div v-if="r.status === 'PENDING'" class="actions">
                <a v-for="a in ACTIONS" :key="a.value" @click="begin(r, a.value)">{{ a.label }}</a>
              </div>
              <span v-else class="muted">{{ r.handleAction }}<template v-if="r.handleNote"> · {{ r.handleNote }}</template></span>
            </td>
          </tr>
          <tr v-if="!list.length">
            <td colspan="7" class="muted">暂无举报记录</td>
          </tr>
        </tbody>
      </table>
    </div>
    <Pagination :total="total" :page="page" :page-size="size" @change="changePage" />
  </section>
</template>
