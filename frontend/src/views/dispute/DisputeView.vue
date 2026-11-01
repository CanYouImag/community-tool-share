<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '@/components/StatusBadge.vue'
import {
  getDispute,
  submitDispute,
  getAward,
  submitReport
} from '@/api/modules/dispute'
import { DISPUTE_STATUS, REPORT_TARGET, REPORT_TYPE } from '@/utils/constants'
import { formatAmount } from '@/utils/format'

const router = useRouter()

const tabs = [
  { id: 'new', label: '发起纠纷' },
  { id: 'progress', label: '处理进度' },
  { id: 'award', label: '裁决书' },
  { id: 'report', label: '举报内容' }
]
const activeTab = ref('new')

const dispute = ref(null)
const award = ref(null)

// 发起纠纷表单
const reason = ref('归还时发现电钻外壳损坏，双方对责任存在分歧。')
const citeEvidence = ref(true)

// 举报表单
const reportTarget = ref('物品')
const reportType = ref('信息不实')
const reportDesc = ref('')
const reportDone = ref(false)

async function load() {
  dispute.value = await getDispute()
  award.value = await getAward()
}

async function doSubmitDispute() {
  if (!reason.value.trim()) return
  await submitDispute({ reason: reason.value, citeEvidence: citeEvidence.value })
  activeTab.value = 'progress'
}

async function doSubmitReport() {
  if (!reportDesc.value.trim()) return
  await submitReport({
    target: reportTarget.value,
    type: reportType.value,
    desc: reportDesc.value
  })
  reportDone.value = true
}

// 从订单详情跳转时定位到对应 tab
function initStageFromHash() {
  const h = router.currentRoute.value.hash.replace('#', '')
  if (tabs.some((t) => t.id === h)) activeTab.value = h
}

onMounted(() => {
  initStageFromHash()
  load()
})
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>纠纷与举报</h1>
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
        @click.prevent="activeTab = t.id"
      >
        {{ t.label }}
      </a>
    </nav>

    <!-- 发起纠纷 -->
    <section v-show="activeTab === 'new'" class="view-panel">
      <h2 class="view-heading">发起纠纷</h2>
      <div class="two">
        <div>
          <section class="panel">
            <div class="panel-title">
              <h2>说明纠纷情况</h2>
            </div>
            <div class="order-product">
              <img class="art" src="/assets/item-30556edc.svg" alt="物品示意图" />
              <div>
                <h3>{{ dispute?.itemTitle }}</h3>
                <p class="muted">{{ dispute?.communityName }} / 出租者 {{ dispute?.lender }}</p>
                <small>10 月 12 日至 14 日 / 租期 3 天</small>
              </div>
            </div>
            <hr class="rule" />
            <label class="field">
              <span>纠纷理由<em>*</em></span>
              <textarea v-model="reason" aria-label="纠纷理由"></textarea>
            </label>
            <label class="upload">
              <strong>＋ 补充凭证</strong>
              <input type="file" accept="image/*" multiple aria-label="补充凭证" />
            </label>
            <label class="check">
              <input v-model="citeEvidence" type="checkbox" />
              引用本订单的取物／归还存证和留言记录
            </label>
            <div class="actions">
              <a class="btn primary" @click="doSubmitDispute">提交纠纷</a>
              <a class="btn" @click="router.push({ name: 'order-detail', params: { id: dispute?.orderId } })">返回订单</a>
            </div>
          </section>
        </div>
      </div>
    </section>

    <!-- 处理进度 -->
    <section v-show="activeTab === 'progress'" class="view-panel">
      <h2 class="view-heading">处理进度</h2>
      <section class="panel">
        <div class="row between">
          <div>
            <small>纠纷编号 {{ dispute?.id }}</small>
            <h2>归还物品损坏争议</h2>
          </div>
          <StatusBadge :status="dispute?.status" />
        </div>
        <div class="info">纠纷处理中，资金已冻结。</div>
        <div class="steps">
          <span class="step active">社区调解</span>
          <span class="step">平台仲裁</span>
        </div>
        <p class="muted">
          当前由「{{ dispute?.mediation?.mediatorRole }} {{ dispute?.mediation?.mediator }}」介入社区调解，
          调解不成立可升级至平台仲裁。
        </p>
      </section>
      <div class="two">
        <div>
          <section class="panel">
            <div class="panel-title">
              <h2>纠纷与证据</h2>
            </div>
            <div class="order-product">
              <img class="art" src="/assets/item-30556edc.svg" alt="物品示意图" />
              <div>
                <h3>{{ dispute?.itemTitle }}</h3>
                <p class="muted">{{ dispute?.communityName }} / 出租者 {{ dispute?.lender }}</p>
                <small>10 月 12 日至 14 日 / 租期 3 天</small>
              </div>
            </div>
            <dl class="line-list">
              <div><dt>发起人</dt><dd>{{ dispute?.initiator }}</dd></div>
              <div><dt>纠纷理由</dt><dd>{{ dispute?.reason }}</dd></div>
              <div><dt>涉及押金</dt><dd>¥{{ formatAmount(dispute?.deposit) }}</dd></div>
            </dl>
            <div class="thumbs">
              <img class="art small" src="/assets/item-30556edc.svg" alt="取物存证示意" />
              <img class="art small" src="/assets/item-30556edc.svg" alt="归还存证示意" />
            </div>
            <label class="upload">
              <strong>＋ 补充证据</strong>
              <input type="file" accept="image/*" multiple aria-label="补充证据" />
            </label>
          </section>
        </div>
        <div>
          <section class="panel">
            <div class="panel-title">
              <h2>处理记录</h2>
            </div>
            <div class="timeline">
              <p v-for="(t, i) in dispute?.timeline" :key="i">
                {{ t.time }} / {{ t.text }}
                <small>{{ t.detail }}</small>
              </p>
            </div>
            <div class="message">
              <header>{{ dispute?.mediatorMessage?.from }}</header>
              {{ dispute?.mediatorMessage?.content }}
            </div>
            <div class="actions">
              <a class="btn" @click="activeTab = 'award'">查看仲裁结果</a>
              <a class="btn" @click="router.push({ name: 'orders', hash: '#borrow' })">返回订单</a>
            </div>
          </section>
        </div>
      </div>
    </section>

    <!-- 裁决书 -->
    <section v-show="activeTab === 'award'" class="view-panel">
      <h2 class="view-heading">裁决书</h2>
      <div class="narrow">
        <section class="panel">
          <article class="receipt">
            <small>邻租平台 / 裁决编号 {{ award?.id }}</small>
            <h2>订单纠纷裁决书</h2>
            <dl class="line-list">
              <div><dt>关联订单</dt><dd>{{ award?.orderId }}</dd></div>
              <div><dt>申请人 / 相对方</dt><dd>{{ award?.applicant }} / {{ award?.counterpart }}</dd></div>
              <div><dt>原押金金额</dt><dd>¥{{ formatAmount(award?.deposit) }}</dd></div>
              <div><dt>裁决类型</dt><dd>{{ award?.type }}</dd></div>
              <div><dt>赔付出租者</dt><dd>¥{{ formatAmount(award?.toLender) }}</dd></div>
              <div><dt>退回租客押金</dt><dd>¥{{ formatAmount(award?.toBorrower) }}</dd></div>
              <div><dt>租金减免</dt><dd>¥{{ formatAmount(award?.rentRelief) }}</dd></div>
            </dl>
            <p>
              <strong>裁决理由</strong><br />
              {{ award?.reason }}
            </p>
            <div class="decision">
              <strong>执行结果：资金划转已完成</strong>
              <p>押金已按上述方案处理。纠纷状态为“已裁决”，订单状态为“已终结”。</p>
              <small>裁决时间：{{ award?.time }} / 裁决人：{{ award?.arbitrator }}</small>
            </div>
          </article>
        </section>
        <div class="actions">
          <a class="btn primary" @click="router.push({ name: 'wallet', hash: '#wallet' })">查看资金流水</a>
          <a class="btn" @click="router.push({ name: 'orders', hash: '#borrow' })">返回订单列表</a>
        </div>
      </div>
    </section>

    <!-- 举报内容 -->
    <section v-show="activeTab === 'report'" class="view-panel">
      <h2 class="view-heading">举报内容</h2>
      <div class="narrow">
        <section class="panel">
          <div class="panel-title">
            <h2>填写举报信息</h2>
          </div>
          <div v-if="reportDone" class="result">
            <div class="result-mark">✓</div>
            <h3>举报已提交</h3>
            <p>管理员将尽快核实处理，感谢你的反馈。</p>
          </div>
          <template v-else>
            <label class="field">
              <span>举报对象</span>
              <select v-model="reportTarget" aria-label="举报对象">
                <option v-for="t in REPORT_TARGET" :key="t">{{ t }}：18V 充电式电钻</option>
              </select>
            </label>
            <label class="field">
              <span>举报类型</span>
              <select v-model="reportType" aria-label="举报类型">
                <option v-for="t in REPORT_TYPE" :key="t">{{ t }}</option>
              </select>
            </label>
            <label class="field">
              <span>举报说明<em>*</em></span>
              <textarea v-model="reportDesc" aria-label="举报说明" placeholder="请具体描述问题，以便管理员核实。"></textarea>
            </label>
            <label class="upload">
              <strong>＋ 上传举报证据</strong>
              <input type="file" accept="image/*" multiple aria-label="上传举报证据" />
            </label>
            <div class="actions">
              <a class="btn primary" @click="doSubmitReport">提交举报</a>
              <a class="btn" @click="activeTab = 'new'">取消</a>
            </div>
          </template>
        </section>
      </div>
    </section>
  </main>
</template>
