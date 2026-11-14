<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import ReportQueue from '@/components/ReportQueue.vue'
import {
  getMyAdminCommunity,
  getCommunityStats,
  getJoinApplies,
  auditJoinApply,
  getMembers,
  kickMember,
  getAnnouncements,
  publishAnnouncement,
  deleteAnnouncement
} from '@/api/modules/admin'
import { getCommunities } from '@/api/modules/community'
import { useAuthStore } from '@/store/auth'
import { formatAmount, formatDateTime } from '@/utils/format'

const route = useRoute()
const auth = useAuthStore()
const isPlatformAdmin = auth.user?.role === 'PLATFORM_ADMIN'
const communities = ref([])
const selectedCommunityId = ref(null)
const communityError = ref('')

const community = ref(null)
const stats = ref(null)

const tabs = [
  { id: 'apply', label: '加入申请' },
  { id: 'members', label: '成员管理' },
  { id: 'notice', label: '公告管理' },
  { id: 'reports', label: '举报处理' }
]
const activeTab = ref('apply')

// 加入申请
const applyStatus = ref('')
const applies = ref([])
const applyTotal = ref(0)
const applyPage = ref(1)
const rejecting = reactive({ id: null, reason: '' })

// 成员
const members = ref([])
const memberTotal = ref(0)
const memberPage = ref(1)

// 公告
const announcements = ref([])
const form = reactive({ title: '', content: '', imageUrl: '' })

async function loadCommunityOptions() {
  if (!isPlatformAdmin) return
  communities.value = await getCommunities()
  const routeId = Number(route.query.communityId || 0)
  if (routeId) selectedCommunityId.value = routeId
}

async function load() {
  communityError.value = ''
  if (isPlatformAdmin && !selectedCommunityId.value) {
    community.value = null
    stats.value = null
    applies.value = []
    members.value = []
    announcements.value = []
    return
  }
  try {
    const selectedId = selectedCommunityId.value || undefined
    community.value = await getMyAdminCommunity(selectedId)
    stats.value = await getCommunityStats(selectedId)
    await Promise.all([loadApplies(), loadMembers(), loadAnnouncements()])
  } catch (e) {
    communityError.value = e?.message || '社区管理数据加载失败'
    community.value = null
  }
}

async function chooseCommunity(id) {
  selectedCommunityId.value = Number(id)
  applyPage.value = 1
  memberPage.value = 1
  await load()
}

async function loadApplies() {
  const data = await getJoinApplies(community.value.id, {
    status: applyStatus.value || undefined,
    page: applyPage.value,
    size: 10
  })
  applies.value = data.records || []
  applyTotal.value = data.total || 0
}

async function loadMembers() {
  const data = await getMembers(community.value.id, { page: memberPage.value, size: 10 })
  members.value = data.records || []
  memberTotal.value = data.total || 0
}

async function loadAnnouncements() {
  announcements.value = await getAnnouncements(community.value.id)
}

function changeApplyPage(p) {
  applyPage.value = p
  loadApplies()
}
function changeMemberPage(p) {
  memberPage.value = p
  loadMembers()
}

async function approve(a) {
  try {
    await auditJoinApply(community.value.id, a.id, { approve: true })
    loadApplies()
  } catch (e) {
    alert(e?.message || '审核失败')
  }
}
function reject(a) {
  rejecting.id = a.id
  rejecting.reason = ''
}
async function confirmReject() {
  try {
    await auditJoinApply(community.value.id, rejecting.id, { approve: false, rejectReason: rejecting.reason })
    rejecting.id = null
    loadApplies()
  } catch (e) {
    alert(e?.message || '审核失败')
  }
}

async function kick(userId) {
  await kickMember(community.value.id, userId)
  loadMembers()
}

async function publish() {
  if (!form.title.trim() || !form.content.trim()) return
  await publishAnnouncement(community.value.id, {
    title: form.title,
    content: form.content,
    imageUrl: form.imageUrl
  })
  form.title = ''
  form.content = ''
  form.imageUrl = ''
  loadAnnouncements()
}
async function removeNotice(id) {
  await deleteAnnouncement(community.value.id, id)
  loadAnnouncements()
}

function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : '成员'
}

onMounted(async () => {
  await loadCommunityOptions()
  await load()
})
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>社区管理</h1>
        <p class="muted">{{ community?.name || (isPlatformAdmin ? '请选择要管理的社区' : '') }}</p>
      </div>
      <label v-if="isPlatformAdmin" class="field">
        <span>选择社区</span>
        <select
          :value="selectedCommunityId || ''"
          aria-label="选择社区"
          @change="chooseCommunity($event.target.value)"
        >
          <option value="" disabled>请选择社区</option>
          <option v-for="c in communities" :key="c.id" :value="c.id">
            {{ c.name }}（#{{ c.id }}）
          </option>
        </select>
      </label>
    </header>

    <p v-if="communityError" class="form-message error">{{ communityError }}</p>
    <EmptyState
      v-if="isPlatformAdmin && !selectedCommunityId"
      text="请先选择社区，再进入成员、公告和加入申请管理"
    />

    <div v-if="community">
    <div class="stats">
      <div class="stat">
        <small>成员数</small>
        <div class="value">{{ stats?.memberCount ?? 0 }}</div>
      </div>
      <div class="stat">
        <small>在租物品</small>
        <div class="value">{{ stats?.itemCount ?? 0 }}</div>
      </div>
      <div class="stat">
        <small>纠纷数</small>
        <div class="value">{{ stats?.disputeCount ?? 0 }}</div>
      </div>
      <div class="stat">
        <small>社区成交额</small>
        <div class="value">¥{{ formatAmount(stats?.gmv) }}</div>
      </div>
    </div>

    <nav class="view-tabs" role="tablist" aria-label="社区管理功能">
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
      <router-link class="view-tab" :to="{ name: 'admin-mediation' }">调解工作台</router-link>
    </nav>

    <!-- 加入申请 -->
    <section v-show="activeTab === 'apply'" class="view-panel">
      <h2 class="view-heading">加入申请</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>审核状态</span>
            <select v-model="applyStatus" aria-label="审核状态" @change="applyPage = 1; loadApplies()">
              <option value="">全部</option>
              <option value="PENDING">待审核</option>
              <option value="APPROVED">已通过</option>
              <option value="REJECTED">已拒绝</option>
            </select>
          </label>
        </div>

        <EmptyState v-if="!applies.length" text="暂无加入申请" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">申请人</th>
                <th scope="col">申请理由</th>
                <th scope="col">状态</th>
                <th scope="col">时间</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in applies" :key="a.id">
                <td>{{ a.nickname }}</td>
                <td>{{ a.applyReason }}</td>
                <td>
                  <StatusBadge
                    :label="a.status === 'PENDING' ? '待审核' : a.status === 'APPROVED' ? '已通过' : '已拒绝'"
                    :cls="a.status === 'PENDING' ? 'warn' : a.status === 'REJECTED' ? 'neutral' : ''"
                  />
                </td>
                <td>{{ formatDateTime(a.createdAt) }}</td>
                <td>
                  <div v-if="rejecting.id === a.id" class="actions">
                    <input v-model="rejecting.reason" type="text" placeholder="拒绝原因（可填）" />
                    <a class="btn small primary" @click="confirmReject">确认拒绝</a>
                    <a class="btn small" @click="rejecting.id = null">取消</a>
                  </div>
                  <div v-else-if="a.status === 'PENDING'" class="actions">
                    <a @click="approve(a)">通过</a>
                    <a @click="reject(a)">拒绝</a>
                  </div>
                  <span v-else class="muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="applyTotal" :page="applyPage" :page-size="10" @change="changeApplyPage" />
      </section>
    </section>

    <!-- 成员管理 -->
    <section v-show="activeTab === 'members'" class="view-panel">
      <h2 class="view-heading">成员管理</h2>
      <section class="panel">
        <EmptyState v-if="!members.length" text="暂无成员" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">成员</th>
                <th scope="col">角色</th>
                <th scope="col">加入时间</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in members" :key="m.userId">
                <td>{{ m.nickname }}</td>
                <td>{{ roleLabel(m.role) }}</td>
                <td>{{ formatDateTime(m.joinedAt) }}</td>
                <td>
                  <a v-if="m.role !== 'ADMIN'" @click="kick(m.userId)">移出社区</a>
                  <span v-else class="muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="memberTotal" :page="memberPage" :page-size="10" @change="changeMemberPage" />
      </section>
    </section>

    <!-- 公告管理 -->
    <section v-show="activeTab === 'notice'" class="view-panel">
      <h2 class="view-heading">公告管理</h2>
      <section class="panel">
        <div class="panel-title">
          <h2>发布公告</h2>
        </div>
        <label class="field">
          <span>标题<em>*</em></span>
          <input v-model="form.title" type="text" aria-label="公告标题" />
        </label>
        <label class="field">
          <span>正文<em>*</em></span>
          <textarea v-model="form.content" aria-label="公告正文"></textarea>
        </label>
        <label class="field">
          <span>配图地址（可填）</span>
          <input v-model="form.imageUrl" type="text" aria-label="配图地址" />
        </label>
        <div class="actions">
          <a class="btn primary" @click="publish">发布</a>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <h2>已发布公告</h2>
        </div>
        <EmptyState v-if="!announcements.length" text="暂无公告" />
        <div v-else class="line-list">
          <div v-for="n in announcements" :key="n.id">
            <div>
              <strong>{{ n.title }}</strong>
              <p class="muted">{{ n.content }}</p>
              <small>{{ formatDateTime(n.createdAt) }}</small>
            </div>
            <a class="danger-text" @click="removeNotice(n.id)">删除</a>
          </div>
        </div>
      </section>
    </section>

    <!-- 举报处理 -->
    <section v-show="activeTab === 'reports'" class="view-panel">
      <h2 class="view-heading">举报处理</h2>
      <ReportQueue />
    </section>
    </div>
  </main>
</template>
