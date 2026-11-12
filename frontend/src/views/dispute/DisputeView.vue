<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/store/auth';

import {
  submitDispute,
  getMyDisputes,
  getDisputeDetail,
  getAward,
  submitReport,
} from '@/api/modules/dispute';
import { getMyMemberships } from '@/api/modules/community';
import { getMyOrders } from '@/api/modules/order';
import StatusBadge from '@/components/StatusBadge.vue';
import ViewTabs from '@/components/ViewTabs.vue';
import { formatAmount, formatDateTime } from '@/utils/format';

const auth = useAuthStore();

const tabs = [
  { id: 'new', label: '发起纠纷' },
  { id: 'mine', label: '我的纠纷' },
  { id: 'award', label: '裁决书' },
  { id: 'report', label: '举报' },
];
const activeTab = ref('new');

// —— 发起纠纷 ——
const communities = ref([]);
const orders = ref([]);
const form = ref({
  orderId: '',
  communityId: '',
  ownerId: '1',
  reason: '',
  depositAmount: '',
  rentAmount: '',
});
const disputeDone = ref(false);

// —— 我的纠纷 ——
const myDisputes = ref([]);
const detail = ref(null);

// —— 裁决书 ——
const awards = ref([]);
const award = ref(null);

// —— 举报 ——
const TARGETS = [
  { value: 'ITEM', label: '物品' },
  { value: 'USER', label: '用户' },
  { value: 'REVIEW', label: '评价' },
];
const TYPES = [
  { value: 'FALSE_INFO', label: '信息不实' },
  { value: 'ILLEGAL_CONTENT', label: '违规内容' },
  { value: 'MALICIOUS_REVIEW', label: '恶意评价' },
  { value: 'OTHER', label: '其他' },
];
const report = ref({ target: 'ITEM', type: 'FALSE_INFO', targetId: '', desc: '' });
const reportDone = ref(false);

async function load() {
  await loadMemberships();
  await loadOrders();
  await loadMyDisputes();
  await loadAwards();
}

async function loadMemberships() {
  const list = await getMyMemberships();
  communities.value = (list || []).filter((c) => c.status === 'JOINED');
  if (!form.value.communityId && communities.value.length) {
    form.value.communityId = String(communities.value[0].id);
  }
}

async function loadOrders() {
  orders.value = (await getMyOrders()) || [];
  if (!form.value.orderId && orders.value.length) {
    form.value.orderId = String(orders.value[0].id);
    onOrderChange();
  }
}

function onOrderChange() {
  const o = orders.value.find((x) => String(x.id) === form.value.orderId);
  if (!o) return;
  form.value.communityId = String(o.communityId);
  form.value.ownerId = String(o.ownerId);
  form.value.depositAmount = o.depositAmount ?? '';
  form.value.rentAmount = o.rentAmount ?? '';
}

async function loadMyDisputes() {
  myDisputes.value = (await getMyDisputes()) || [];
  detail.value = null;
}

async function loadAwards() {
  awards.value = myDisputes.value.filter((d) => d.status === 'ARBITRATED');
}

async function doSubmitDispute() {
  if (!form.value.orderId || !form.value.communityId || !form.value.ownerId || !form.value.reason.trim()) return;
  await submitDispute({
    orderId: Number(form.value.orderId),
    communityId: Number(form.value.communityId),
    renterId: auth.user?.id,
    ownerId: Number(form.value.ownerId),
    reason: form.value.reason.trim(),
    depositAmount: form.value.depositAmount === '' ? null : Number(form.value.depositAmount),
    rentAmount: form.value.rentAmount === '' ? null : Number(form.value.rentAmount),
  });
  disputeDone.value = true;
  form.value.reason = '';
  await loadMyDisputes();
  await loadAwards();
}

async function openDetail(d) {
  detail.value = await getDisputeDetail(d.id);
}

async function openAward(d) {
  award.value = await getAward(d.id);
}

async function doSubmitReport() {
  if (!report.value.desc.trim()) return;
  await submitReport({
    target: report.value.target,
    type: report.value.type,
    targetId: report.value.targetId === '' ? null : Number(report.value.targetId),
    desc: report.value.desc.trim(),
  });
  reportDone.value = true;
  report.value.desc = '';
}

onMounted(load);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>纠纷与举报</h1>
    </header>

    <ViewTabs v-model="activeTab" :tabs="tabs" label="纠纷与举报" />

    <!-- 发起纠纷 -->
    <section v-show="activeTab === 'new'" class="view-panel">
      <div class="narrow">
        <section class="panel">
          <div class="panel-title"><h2>发起纠纷</h2></div>
          <div v-if="disputeDone" class="result">
            <div class="result-mark">✓</div>
            <h3>纠纷已提交</h3>
            <p>已进入社区调解，可在「我的纠纷」查看进度。</p>
            <a class="btn" @click="disputeDone = false">继续发起</a>
          </div>
          <template v-else>
            <label class="field">
              <span>关联订单<em>*</em></span>
              <select v-model="form.orderId" aria-label="关联订单" @change="onOrderChange">
                <option v-for="o in orders" :key="o.id" :value="String(o.id)">
                  订单 #{{ o.id }} · {{ o.itemTitle }}（押金 ¥{{ formatAmount(o.depositAmount) }} / 租金 ¥{{ formatAmount(o.rentAmount) }}）
                </option>
              </select>
              <small v-if="!orders.length" class="muted">你还没有可发起纠纷的订单。</small>
            </label>
            <label class="field">
              <span>所属社区<em>*</em></span>
              <select v-model="form.communityId" aria-label="所属社区">
                <option v-for="c in communities" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
              </select>
              <small v-if="!communities.length" class="muted">你还没有加入任何社区，先去「社区」页加入。</small>
            </label>
            <label class="field">
              <span>出租者用户 ID<em>*</em></span>
              <input v-model="form.ownerId" type="number" aria-label="出租者用户 ID" disabled />
              <small class="muted">由所选订单自动带入。</small>
            </label>
            <label class="field">
              <span>纠纷理由<em>*</em></span>
              <textarea v-model="form.reason" aria-label="纠纷理由" placeholder="请描述争议情况"></textarea>
            </label>
            <div class="two">
              <label class="field">
                <span>押金金额（可选）</span>
                <input v-model="form.depositAmount" type="number" min="0" step="0.01" aria-label="押金金额" />
              </label>
              <label class="field">
                <span>租金金额（可选）</span>
                <input v-model="form.rentAmount" type="number" min="0" step="0.01" aria-label="租金金额" />
              </label>
            </div>
            <div class="actions">
              <a class="btn primary" @click="doSubmitDispute">提交纠纷</a>
            </div>
          </template>
        </section>
      </div>
    </section>

    <!-- 我的纠纷 -->
    <section v-show="activeTab === 'mine'" class="view-panel">
      <h2 class="view-heading">我的纠纷</h2>
      <section class="panel">
        <p v-if="!myDisputes.length" class="muted">暂无纠纷记录。</p>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">编号 / 订单</th>
                <th scope="col">社区</th>
                <th scope="col">理由</th>
                <th scope="col">押金</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in myDisputes" :key="d.id">
                <td>{{ d.id }}<small>订单 #{{ d.orderId }}</small></td>
                <td>{{ d.communityName }}</td>
                <td>{{ d.reason }}</td>
                <td>¥{{ formatAmount(d.deposit) }}</td>
                <td><StatusBadge :status="d.status" /></td>
                <td><a @click="openDetail(d)">查看详情</a></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

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
                <div><dt>出租者</dt><dd>{{ detail.ownerName }}</dd></div>
                <div><dt>纠纷理由</dt><dd>{{ detail.reason }}</dd></div>
                <div><dt>押金 / 租金</dt><dd>¥{{ formatAmount(detail.deposit) }} / ¥{{ formatAmount(detail.rent) }}</dd></div>
                <div v-if="detail.mediationConclusion"><dt>调解结论</dt><dd>{{ detail.mediationConclusion }}</dd></div>
                <div v-if="detail.escalateNote"><dt>上报说明</dt><dd>{{ detail.escalateNote }}</dd></div>
              </dl>
              <p v-if="detail.evidences?.length" class="muted">已提交举证 {{ detail.evidences.length }} 份</p>
            </section>
          </div>
          <div>
            <section class="panel">
              <div class="panel-title"><h2>处理记录</h2></div>
              <div v-if="detail.messages?.length" class="timeline">
                <p v-for="m in detail.messages" :key="m.id">
                  {{ m.senderName }}
                  <small>{{ formatDateTime(m.createdAt) }}</small>
                  <span>{{ m.content }}</span>
                </p>
              </div>
              <p v-else class="muted">暂无留言</p>
            </section>
            <section v-if="detail.status === 'ARBITRATED'" class="panel">
              <div class="actions">
                <a class="btn primary" @click="openAward(detail)">查看裁决书</a>
              </div>
            </section>
          </div>
        </div>
      </section>
    </section>

    <!-- 裁决书 -->
    <section v-show="activeTab === 'award'" class="view-panel">
      <div class="narrow">
        <section v-if="!award" class="panel">
          <div class="panel-title"><h2>已裁决纠纷</h2></div>
          <p v-if="!awards.length" class="muted">暂无已裁决的纠纷。</p>
          <div v-else class="line-list">
            <div v-for="d in awards" :key="d.id">
              <div>
                <strong>订单 #{{ d.orderId }} · {{ d.communityName }}</strong>
                <p class="muted">{{ d.reason }}</p>
              </div>
              <a class="btn" @click="openAward(d)">查看裁决书</a>
            </div>
          </div>
        </section>
        <section v-else class="panel">
          <article class="receipt">
            <small>邻租平台 / 裁决编号 {{ award.id }}</small>
            <h2>订单纠纷裁决书</h2>
            <dl class="line-list">
              <div><dt>关联订单</dt><dd>{{ award.orderId }}</dd></div>
              <div><dt>申请人 / 相对方</dt><dd>{{ award.applicant }} / {{ award.counterpart }}</dd></div>
              <div><dt>裁决类型</dt><dd>{{ award.type }}</dd></div>
              <div><dt>赔付出租者</dt><dd>¥{{ formatAmount(award.toLender) }}</dd></div>
              <div><dt>退回租客押金</dt><dd>¥{{ formatAmount(award.toBorrower) }}</dd></div>
              <div><dt>租金减免</dt><dd>¥{{ formatAmount(award.rentRelief) }}</dd></div>
            </dl>
            <p><strong>裁决理由</strong><br />{{ award.reason }}</p>
            <div class="decision">
              <strong>执行结果：资金划转已完成</strong>
              <small>裁决时间：{{ formatDateTime(award.time) }} / 裁决人：{{ award.arbitrator }}</small>
            </div>
          </article>
          <div class="actions">
            <a class="btn" @click="award = null">返回列表</a>
          </div>
        </section>
      </div>
    </section>

    <!-- 举报 -->
    <section v-show="activeTab === 'report'" class="view-panel">
      <div class="narrow">
        <section class="panel">
          <div class="panel-title"><h2>提交举报</h2></div>
          <div v-if="reportDone" class="result">
            <div class="result-mark">✓</div>
            <h3>举报已提交</h3>
            <p>管理员将尽快核实处理，感谢你的反馈。</p>
            <a class="btn" @click="reportDone = false">继续举报</a>
          </div>
          <template v-else>
            <label class="field">
              <span>举报对象</span>
              <select v-model="report.target" aria-label="举报对象">
                <option v-for="t in TARGETS" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </label>
            <label class="field">
              <span>举报类型</span>
              <select v-model="report.type" aria-label="举报类型">
                <option v-for="t in TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </label>
            <label class="field">
              <span>目标 ID（可选）</span>
              <input v-model="report.targetId" type="number" aria-label="目标 ID" />
            </label>
            <label class="field">
              <span>举报说明<em>*</em></span>
              <textarea v-model="report.desc" aria-label="举报说明" placeholder="请具体描述问题"></textarea>
            </label>
            <div class="actions">
              <a class="btn primary" @click="doSubmitReport">提交举报</a>
            </div>
          </template>
        </section>
      </div>
    </section>
  </main>
</template>
