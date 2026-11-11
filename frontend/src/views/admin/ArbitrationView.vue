<script setup>
import { ref, reactive, onMounted } from 'vue'
import StatusBadge from '@/components/StatusBadge.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getDisputes, getDisputeDetail, arbitrateDispute } from '@/api/modules/admin'
import { formatAmount, formatDateTime } from '@/utils/format'

const VERDICT_OPTIONS = [
  { value: 'REFUND_ALL', label: '全退租客（押金全额退回租客）' },
  { value: 'PARTIAL_DEDUCT', label: '部分扣除（按金额划付出租者）' },
  { value: 'DEDUCT_ALL', label: '全额扣除（押金全额给出租者）' }
]

const status = ref('PENDING_ARBITRATION')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const detail = ref(null)
const form = reactive({ verdictType: 'PARTIAL_DEDUCT', deductAmount: '', rentDiscount: '', awardReason: '' })

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
  form.verdictType = 'PARTIAL_DEDUCT'
  form.deductAmount = ''
  form.rentDiscount = ''
  form.awardReason = ''
}

async function doArbitrate() {
  if (!detail.value || !form.awardReason.trim()) return
  const payload = {
    verdictType: form.verdictType,
    rentDiscount: form.rentDiscount === '' ? 0 : Number(form.rentDiscount),
    awardReason: form.awardReason
  }
  if (form.verdictType === 'PARTIAL_DEDUCT') {
    payload.deductAmount = form.deductAmount === '' ? 0 : Number(form.deductAmount)
  }
  await arbitrateDispute(detail.value.id, payload)
  await select(detail.value)
  load()
}

onMounted(load)
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>仲裁中心</h1>
      </div>
    </header>

    <section class="view-panel">
      <h2 class="view-heading">待仲裁纠纷</h2>
      <section class="panel">
        <div class="filters">
          <label class="field">
            <span>纠纷状态</span>
            <select v-model="status" aria-label="纠纷状态" @change="page = 1; load()">
              <option value="PENDING_ARBITRATION">待仲裁</option>
              <option value="ARBITRATED">已裁决</option>
              <option value="">全部</option>
            </select>
          </label>
        </div>

        <EmptyState v-if="!list.length" text="暂无待仲裁纠纷" />
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">编号 / 订单</th>
                <th scope="col">社区</th>
                <th scope="col">当事人</th>
                <th scope="col">押金</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in list" :key="d.id">
                <td>{{ d.id }}<small>订单 #{{ d.orderId }}</small></td>
                <td>{{ d.communityName }}</td>
                <td>{{ d.renterName }} / {{ d.ownerName }}</td>
                <td>¥{{ formatAmount(d.deposit) }}</td>
                <td><StatusBadge :status="d.status" /></td>
                <td><a @click="select(d)">{{ d.status === 'PENDING_ARBITRATION' ? '裁决' : '查看裁决书' }}</a></td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination :total="total" :page="page" :page-size="size" @change="changePage" />
      </section>
    </section>

    <!-- 详情与裁决 -->
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
              <div v-if="detail.escalateNote"><dt>上报说明</dt><dd>{{ detail.escalateNote }}</dd></div>
            </dl>
            <div v-if="detail.evidences?.length" class="thumbs">
              <img v-for="e in detail.evidences" :key="e.id" class="art small" :src="e.imageUrl" :alt="e.evidenceType" />
            </div>
          </section>

          <section v-if="detail.award" class="panel">
            <article class="receipt">
              <small>邻租平台 / 裁决编号 {{ detail.award.id }}</small>
              <h2>订单纠纷裁决书</h2>
              <dl class="line-list">
                <div><dt>关联订单</dt><dd>{{ detail.award.orderId }}</dd></div>
                <div><dt>申请人 / 相对方</dt><dd>{{ detail.award.applicant }} / {{ detail.award.counterpart }}</dd></div>
                <div><dt>裁决类型</dt><dd>{{ detail.award.type }}</dd></div>
                <div><dt>赔付出租者</dt><dd>¥{{ formatAmount(detail.award.toLender) }}</dd></div>
                <div><dt>退回租客押金</dt><dd>¥{{ formatAmount(detail.award.toBorrower) }}</dd></div>
                <div><dt>租金减免</dt><dd>¥{{ formatAmount(detail.award.rentRelief) }}</dd></div>
              </dl>
              <p>
                <strong>裁决理由</strong><br />
                {{ detail.award.reason }}
              </p>
              <small>裁决时间：{{ formatDateTime(detail.award.time) }} / 裁决人：{{ detail.award.arbitrator }}</small>
            </article>
          </section>
        </div>

        <div>
          <section v-if="detail.status === 'PENDING_ARBITRATION'" class="panel">
            <div class="panel-title">
              <h2>作出裁决</h2>
            </div>
            <label class="field">
              <span>裁决类型</span>
              <select v-model="form.verdictType" aria-label="裁决类型">
                <option v-for="o in VERDICT_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </label>
            <label v-if="form.verdictType === 'PARTIAL_DEDUCT'" class="field">
              <span>扣除押金金额（划付出租者）</span>
              <input v-model="form.deductAmount" type="number" min="0" step="0.01" aria-label="扣除押金金额" />
            </label>
            <label class="field">
              <span>租金减免（可填）</span>
              <input v-model="form.rentDiscount" type="number" min="0" step="0.01" aria-label="租金减免" />
            </label>
            <label class="field">
              <span>裁决理由<em>*</em></span>
              <textarea v-model="form.awardReason" aria-label="裁决理由"></textarea>
            </label>
            <div class="actions">
              <a class="btn primary" @click="doArbitrate">提交裁决并划转资金</a>
            </div>
          </section>

          <section v-if="detail.messages?.length" class="panel">
            <div class="panel-title">
              <h2>处理记录</h2>
            </div>
            <div class="timeline">
              <p v-for="m in detail.messages" :key="m.id">
                {{ m.senderName }}（{{ m.senderRole }}）
                <small>{{ formatDateTime(m.createdAt) }}</small>
                <span>{{ m.content }}</span>
              </p>
            </div>
          </section>
        </div>
      </div>
    </section>
  </main>
</template>
