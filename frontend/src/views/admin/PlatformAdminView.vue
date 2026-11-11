<script setup>
import { ref, reactive, onMounted } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getPlatformCommunities,
  createCommunity,
  updateCommunity,
  appointAdmin,
  dismissAdmin,
  getUsers,
  banUser,
  unbanUser,
  getConfig,
  updateConfig,
  getPlatformStats,
  getLogs
} from '@/api/modules/admin'
import { formatAmount, formatDateTime } from '@/utils/format'

const ROLE_LABEL = {
  REGISTERED: '已注册',
  CERTIFIED: '已认证',
  COMMUNITY_ADMIN: '社区管理员',
  PLATFORM_ADMIN: '平台管理员'
}

const tabs = [
  { id: 'communities', label: '社区管理' },
  { id: 'users', label: '用户管理' },
  { id: 'config', label: '参数配置' },
  { id: 'stats', label: '数据统计' },
  { id: 'logs', label: '操作日志' }
]
const activeTab = ref('communities')

// —— 社区 ——
const communities = ref([])
const editing = reactive({ id: null, name: '', intro: '', coverUrl: '', autoApprove: false, status: 'ACTIVE' })
const appoint = reactive({ communityId: null, userId: '' })

async function loadCommunities() {
  communities.value = await getPlatformCommunities()
}

function resetForm() {
  editing.id = null
  editing.name = ''
  editing.intro = ''
  editing.coverUrl = ''
  editing.autoApprove = false
  editing.status = 'ACTIVE'
}
function beginCreate() {
  resetForm()
}
function beginEdit(c) {
  editing.id = c.id
  editing.name = c.name
  editing.intro = c.intro || ''
  editing.coverUrl = c.coverUrl || ''
  editing.autoApprove = c.autoApprove
  editing.status = c.status
}
async function saveCommunity() {
  if (!editing.name.trim()) return
  const payload = {
    name: editing.name,
    intro: editing.intro,
    coverUrl: editing.coverUrl,
    autoApprove: editing.autoApprove
  }
  if (editing.id) {
    await updateCommunity(editing.id, { ...payload, status: editing.status })
  } else {
    await createCommunity(payload)
  }
  resetForm()
  loadCommunities()
}
function beginAppoint(c) {
  appoint.communityId = c.id
  appoint.userId = ''
}
async function doAppoint() {
  if (!appoint.userId) return
  await appointAdmin(appoint.communityId, { userId: Number(appoint.userId) })
  appoint.communityId = null
  appoint.userId = ''
  loadCommunities()
}
async function doDismiss(c) {
  await dismissAdmin(c.id, c.adminId)
  loadCommunities()
}

// —— 用户 ——
const userKeyword = ref('')
const userStatus = ref('')
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const ban = reactive({ userId: null, reason: '' })

async function loadUsers() {
  const data = await getUsers({
    keyword: userKeyword.value || undefined,
    status: userStatus.value || undefined,
    page: userPage.value,
    size: 10
  })
  users.value = data.records || []
  userTotal.value = data.total || 0
}
function changeUserPage(p) {
  userPage.value = p
  loadUsers()
}
function beginBan(u) {
  ban.userId = u.id
  ban.reason = ''
}
async function doBan() {
  await banUser(ban.userId, { reason: ban.reason })
  ban.userId = null
  loadUsers()
}
async function doUnban(u) {
  await unbanUser(u.id, { reason: '' })
  loadUsers()
}

// —— 参数 ——
const configForm = reactive({ commissionRate: '', communityAutoApprove: false, itemAuditEnabled: true })
async function loadConfig() {
  const c = await getConfig()
  configForm.commissionRate = c.commissionRate != null ? String(c.commissionRate) : ''
  configForm.communityAutoApprove = !!c.communityAutoApprove
  configForm.itemAuditEnabled = !!c.itemAuditEnabled
}
async function saveConfig() {
  const payload = {
    communityAutoApprove: configForm.communityAutoApprove,
    itemAuditEnabled: configForm.itemAuditEnabled
  }
  if (configForm.commissionRate !== '') payload.commissionRate = Number(configForm.commissionRate)
  await updateConfig(payload)
  loadConfig()
}

// —— 统计 ——
const stats = ref(null)
async function loadStats() {
  stats.value = await getPlatformStats()
}

// —— 日志 ——
const logAction = ref('')
const logs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
async function loadLogs() {
  const data = await getLogs({ action: logAction.value || undefined, page: logPage.value, size: 10 })
  logs.value = data.records || []
  logTotal.value = data.total || 0
}
function changeLogPage(p) {
  logPage.value = p
  loadLogs()
}

onMounted(async () => {
  await Promise.all([loadCommunities(), loadUsers(), loadConfig(), loadStats(), loadLogs()])
})
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>平台管理</h1>
      </div>
    </header>

    <nav class="view-tabs" role="tablist" aria-label="平台管理功能">
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

    <!-- 社区管理 -->
    <section v-show="activeTab === 'communities'" class="view-panel">
      <h2 class="view-heading">社区管理</h2>
      <section class="panel">
        <div class="panel-title">
          <h2>{{ editing.id ? '编辑社区' : '新增社区' }}</h2>
        </div>
        <div class="two">
          <div>
            <label class="field">
              <span>社区名称<em>*</em></span>
              <input v-model="editing.name" type="text" aria-label="社区名称" />
            </label>
            <label class="field">
              <span>社区简介</span>
              <textarea v-model="editing.intro" aria-label="社区简介"></textarea>
            </label>
            <label class="field">
              <span>封面图地址</span>
              <input v-model="editing.coverUrl" type="text" aria-label="封面图地址" />
            </label>
          </div>
          <div>
            <label class="check">
              <input v-model="editing.autoApprove" type="checkbox" />
              开启自动通过入社申请
            </label>
            <label v-if="editing.id" class="field">
              <span>社区状态</span>
              <select v-model="editing.status" aria-label="社区状态">
                <option value="ACTIVE">正常</option>
                <option value="DISABLED">停用</option>
              </select>
            </label>
            <div class="actions">
              <a class="btn primary" @click="saveCommunity">保存</a>
              <a v-if="editing.id" class="btn" @click="resetForm">取消编辑</a>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <h2>社区列表</h2>
        </div>
        <EmptyState v-if="!communities.length" text="暂无社区" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">社区</th>
                <th scope="col">成员 / 物品</th>
                <th scope="col">状态</th>
                <th scope="col">社区管理员</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in communities" :key="c.id">
                <td>{{ c.name }}<small>{{ c.intro }}</small></td>
                <td>{{ c.memberCount }} / {{ c.itemCount }}</td>
                <td>
                  <StatusBadge
                    :label="c.status === 'ACTIVE' ? '正常' : '停用'"
                    :cls="c.status === 'ACTIVE' ? '' : 'neutral'"
                  />
                </td>
                <td>{{ c.adminName || '未任命' }}</td>
                <td>
                  <div class="actions">
                    <a @click="beginEdit(c)">编辑</a>
                    <a v-if="appoint.communityId !== c.id" @click="beginAppoint(c)">任命管理员</a>
                    <a v-if="c.adminId" class="danger-text" @click="doDismiss(c)">罢免</a>
                  </div>
                  <div v-if="appoint.communityId === c.id" class="actions">
                    <input v-model="appoint.userId" type="text" placeholder="输入用户 ID" />
                    <a class="btn small primary" @click="doAppoint">确认</a>
                    <a class="btn small" @click="appoint.communityId = null">取消</a>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </section>

    <!-- 用户管理 -->
    <section v-show="activeTab === 'users'" class="view-panel">
      <h2 class="view-heading">用户管理</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>账号 / 昵称</span>
            <input v-model="userKeyword" type="text" aria-label="账号 / 昵称" @keyup.enter="userPage = 1; loadUsers()" />
          </label>
          <label class="field">
            <span>状态</span>
            <select v-model="userStatus" aria-label="用户状态" @change="userPage = 1; loadUsers()">
              <option value="">全部</option>
              <option value="ACTIVE">正常</option>
              <option value="BANNED">已封禁</option>
            </select>
          </label>
          <div class="actions">
            <a class="btn primary" @click="userPage = 1; loadUsers()">搜索</a>
          </div>
        </div>

        <EmptyState v-if="!users.length" text="暂无用户" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">用户</th>
                <th scope="col">角色</th>
                <th scope="col">信用分</th>
                <th scope="col">状态</th>
                <th scope="col">注册时间</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.nickname }}<small>{{ u.username }} #{{ u.id }}</small></td>
                <td>{{ ROLE_LABEL[u.role] || u.role }}</td>
                <td>{{ u.creditScore ?? '—' }}</td>
                <td>
                  <StatusBadge
                    :label="u.status === 'BANNED' ? '已封禁' : '正常'"
                    :cls="u.status === 'BANNED' ? 'error' : ''"
                  />
                </td>
                <td>{{ formatDateTime(u.createdAt) }}</td>
                <td>
                  <div v-if="ban.userId === u.id" class="actions">
                    <input v-model="ban.reason" type="text" placeholder="封禁原因（可填）" />
                    <a class="btn small primary" @click="doBan">确认封禁</a>
                    <a class="btn small" @click="ban.userId = null">取消</a>
                  </div>
                  <div v-else class="actions">
                    <a v-if="u.status !== 'BANNED'" class="danger-text" @click="beginBan(u)">封禁</a>
                    <a v-else @click="doUnban(u)">解封</a>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="userTotal" :page="userPage" :page-size="10" @change="changeUserPage" />
      </section>
    </section>

    <!-- 参数配置 -->
    <section v-show="activeTab === 'config'" class="view-panel">
      <h2 class="view-heading">参数配置</h2>
      <div class="narrow">
        <section class="panel">
          <label class="field">
            <span>平台佣金比例（0~0.5）</span>
            <input v-model="configForm.commissionRate" type="number" step="0.01" min="0" max="0.5" aria-label="佣金比例" />
          </label>
          <label class="check">
            <input v-model="configForm.communityAutoApprove" type="checkbox" />
            社区入社申请自动通过
          </label>
          <label class="check">
            <input v-model="configForm.itemAuditEnabled" type="checkbox" />
            物品发布需平台审核
          </label>
          <div class="actions">
            <a class="btn primary" @click="saveConfig">保存配置</a>
          </div>
        </section>
      </div>
    </section>

    <!-- 数据统计 -->
    <section v-show="activeTab === 'stats'" class="view-panel">
      <h2 class="view-heading">数据统计</h2>
      <div class="stats">
        <div class="stat">
          <small>注册用户</small>
          <div class="value">{{ stats?.userCount ?? 0 }}</div>
        </div>
        <div class="stat">
          <small>社区数量</small>
          <div class="value">{{ stats?.communityCount ?? 0 }}</div>
        </div>
        <div class="stat">
          <small>纠纷总数</small>
          <div class="value">{{ stats?.disputeCount ?? 0 }}</div>
        </div>
        <div class="stat">
          <small>平台成交额</small>
          <div class="value">¥{{ formatAmount(stats?.gmv) }}</div>
        </div>
      </div>
    </section>

    <!-- 操作日志 -->
    <section v-show="activeTab === 'logs'" class="view-panel">
      <h2 class="view-heading">操作日志</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>操作类型</span>
            <input v-model="logAction" type="text" aria-label="操作类型" placeholder="如 BAN_USER" @keyup.enter="logPage = 1; loadLogs()" />
          </label>
          <div class="actions">
            <a class="btn primary" @click="logPage = 1; loadLogs()">查询</a>
          </div>
        </div>

        <EmptyState v-if="!logs.length" text="暂无日志" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">操作人</th>
                <th scope="col">操作</th>
                <th scope="col">对象</th>
                <th scope="col">详情</th>
                <th scope="col">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="l in logs" :key="l.id">
                <td>{{ l.operatorName }}</td>
                <td>{{ l.action }}</td>
                <td>{{ l.targetType }}<template v-if="l.targetId"> #{{ l.targetId }}</template></td>
                <td>{{ l.detail }}</td>
                <td>{{ formatDateTime(l.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="logTotal" :page="logPage" :page-size="10" @change="changeLogPage" />
      </section>
    </section>
  </main>
</template>
