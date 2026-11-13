<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

import {
  acceptOrder,
  cancelOrder,
  confirmReturn,
  getOrder,
  getOrderMessages,
  inspectOrder,
  payOrder,
  pickupOrder,
  postOrderMessage,
  rejectOrder,
  returnOrder,
} from '@/api/modules/order';
import { getWallet } from '@/api/modules/wallet';
import { useAuthStore } from '@/store/auth';
import { formatDateTime, formatMoney } from '@/utils/format';

const route = useRoute();
const auth = useAuthStore();

const order = ref(null);
const messages = ref([]);
const wallet = ref(null);
const loading = ref(true);
const error = ref('');
const actionError = ref('');
const newMessage = ref('');
const pickupCode = ref('');

const statusLabels = {
  PENDING_PAYMENT: '待支付',
  PENDING_ACCEPT: '待接单',
  PENDING_PICKUP: '待取物',
  IN_USE: '租用中',
  PENDING_RETURN: '待归还',
  PENDING_INSPECT: '待验收',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  DISPUTED: '纠纷中',
};

const isRenter = computed(
  () => Boolean(order.value) && auth.user?.id === order.value.renterId,
);
const isLessor = computed(
  () => Boolean(order.value) && auth.user?.id === order.value.lessorId,
);
const orderTotal = computed(
  () => Number(order.value?.rentAmount || 0) + Number(order.value?.depositAmount || 0),
);
const statusLabel = computed(
  () => statusLabels[order.value?.status] || order.value?.status || '—',
);

async function load() {
  loading.value = true;
  error.value = '';
  actionError.value = '';
  try {
    const id = route.params.id;
    order.value = await getOrder(id);
    messages.value = await getOrderMessages(id);
    try {
      wallet.value = await getWallet();
    } catch {
      wallet.value = null;
    }
  } catch (err) {
    error.value = err.message || '订单加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function run(action) {
  actionError.value = '';
  try {
    await action();
    await load();
  } catch (err) {
    actionError.value = err.message || '操作失败，请稍后重试';
  }
}

function doPay() {
  return run(() => payOrder(order.value.id));
}

function doAccept() {
  return run(() => acceptOrder(order.value.id));
}

function doReject() {
  const reason = window.prompt('请输入拒单原因', '时间冲突');
  if (reason === null) return;
  return run(() => rejectOrder(order.value.id, reason));
}

function doPickup() {
  if (!pickupCode.value.trim()) {
    actionError.value = '请输入租客出示的取物码';
    return;
  }
  return run(async () => {
    await pickupOrder(order.value.id, pickupCode.value.trim());
    pickupCode.value = '';
  });
}

function doReturn() {
  return run(() => returnOrder(order.value.id));
}

function doConfirmReturn() {
  return run(() => confirmReturn(order.value.id));
}

function doInspect(pass) {
  let reason = '';
  if (!pass) {
    reason = window.prompt('请输入验收不通过原因');
    if (reason === null) return;
  }
  return run(() => inspectOrder(order.value.id, pass, reason));
}

function doCancel() {
  const reason = window.prompt('请输入取消原因', '行程有变');
  if (reason === null) return;
  return run(() => cancelOrder(order.value.id, reason));
}

async function sendMessage() {
  const content = newMessage.value.trim();
  if (!content) return;
  actionError.value = '';
  try {
    await postOrderMessage(order.value.id, content);
    newMessage.value = '';
    messages.value = await getOrderMessages(order.value.id);
  } catch (err) {
    actionError.value = err.message || '留言发送失败';
  }
}

onMounted(load);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>订单详情与操作</h1>
      <router-link class="btn" :to="{ name: 'orders' }">返回我的订单</router-link>
    </header>

    <section v-if="loading" class="panel" role="status">正在加载订单…</section>

    <section v-else-if="error" class="panel" role="alert">
      <h2>暂时无法查看订单</h2>
      <p>{{ error }}</p>
    </section>

    <template v-else-if="order">
      <section class="panel">
        <div class="panel-title">
          <h2>{{ order.itemTitle }}</h2>
          <span class="badge">{{ statusLabel }}</span>
        </div>
        <p class="muted">订单号 {{ order.orderNo }}</p>

        <dl class="line-list">
          <div>
            <dt>租期</dt>
            <dd>{{ order.startDate }} 至 {{ order.endDate }}（{{ order.days }} 天，含首尾）</dd>
          </div>
          <div>
            <dt>租金</dt>
            <dd>{{ formatMoney(order.rentAmount) }}</dd>
          </div>
          <div>
            <dt>押金</dt>
            <dd>{{ formatMoney(order.depositAmount) }}</dd>
          </div>
          <div>
            <dt>合计</dt>
            <dd>{{ formatMoney(orderTotal) }}</dd>
          </div>
          <div>
            <dt>出租者</dt>
            <dd>{{ order.lessorNickname || `#${order.lessorId}` }}</dd>
          </div>
          <div>
            <dt>租客</dt>
            <dd>{{ order.renterNickname || `#${order.renterId}` }}</dd>
          </div>
          <div v-if="order.commissionAmount != null">
            <dt>平台佣金</dt>
            <dd>{{ formatMoney(order.commissionAmount) }}</dd>
          </div>
          <div v-if="order.cancelReason">
            <dt>取消 / 纠纷原因</dt>
            <dd>{{ order.cancelReason }}</dd>
          </div>
        </dl>

        <p v-if="actionError" class="form-message error" role="alert">{{ actionError }}</p>

        <div v-if="order.status === 'PENDING_PAYMENT' && isRenter" class="actions">
          <button class="btn primary" type="button" @click="doPay">
            确认支付 {{ formatMoney(orderTotal) }}
          </button>
          <button class="btn" type="button" @click="doCancel">取消订单</button>
        </div>

        <div v-if="order.status === 'PENDING_ACCEPT' && isLessor" class="actions">
          <button class="btn primary" type="button" @click="doAccept">确认接单</button>
          <button class="btn" type="button" @click="doReject">拒绝接单</button>
        </div>

        <div v-if="order.status === 'PENDING_ACCEPT' && isRenter" class="actions">
          <button class="btn" type="button" @click="doCancel">取消订单</button>
        </div>

        <template v-if="order.status === 'PENDING_PICKUP'">
          <hr class="rule" />
          <h3>取物交接</h3>
          <p v-if="order.pickupCode && isRenter">
            取物码：<strong class="price">{{ order.pickupCode }}</strong>
          </p>
          <div v-if="isLessor" class="row">
            <input
              v-model="pickupCode"
              class="grow"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="输入租客出示的取物码"
            />
            <button class="btn primary" type="button" @click="doPickup">核销并交接</button>
          </div>
          <p v-else class="muted">请向出租者出示取物码，由出租者核销。</p>
        </template>

        <div v-if="order.status === 'IN_USE' && isRenter" class="actions">
          <button class="btn primary" type="button" @click="doReturn">发起归还</button>
        </div>

        <div v-if="order.status === 'PENDING_RETURN' && isLessor" class="actions">
          <button class="btn primary" type="button" @click="doConfirmReturn">确认收到归还</button>
        </div>

        <div v-if="order.status === 'PENDING_INSPECT' && isLessor" class="actions">
          <button class="btn primary" type="button" @click="doInspect(true)">
            验收通过并结算
          </button>
          <button class="btn" type="button" @click="doInspect(false)">验收不通过</button>
        </div>

        <p v-if="order.status === 'PENDING_RETURN' && isRenter" class="muted">
          已发起归还，等待出租者确认。
        </p>
        <p v-if="order.status === 'PENDING_INSPECT' && isRenter" class="muted">
          等待出租者验收。
        </p>
        <p v-if="order.status === 'COMPLETED'" class="form-message">
          订单已完成，押金已按结算结果处理。
        </p>
        <p v-if="order.status === 'DISPUTED'" class="form-message error">
          订单已进入纠纷流程，请到纠纷页面查看处理进度。
        </p>
      </section>

      <div class="two">
        <section class="panel">
          <div class="panel-title">
            <h2>费用明细</h2>
          </div>
          <dl class="line-list">
            <div>
              <dt>日租金</dt>
              <dd>{{ formatMoney(Number(order.rentAmount || 0) / Math.max(1, order.days || 1)) }}</dd>
            </div>
            <div>
              <dt>租期</dt>
              <dd>{{ order.days }} 天</dd>
            </div>
            <div>
              <dt>租金</dt>
              <dd>{{ formatMoney(order.rentAmount) }}</dd>
            </div>
            <div>
              <dt>订单押金</dt>
              <dd>{{ formatMoney(order.depositAmount) }}</dd>
            </div>
            <div>
              <dt>当前可用余额</dt>
              <dd>{{ wallet ? formatMoney(wallet.balance) : '—' }}</dd>
            </div>
          </dl>
          <div class="total">
            <span>合计支付</span>
            <span class="price">{{ formatMoney(orderTotal) }}</span>
          </div>
          <router-link :to="{ name: 'wallet', hash: '#wallet' }">查看资金流水</router-link>
        </section>

        <section class="panel">
          <div class="panel-title">
            <h2>时间记录</h2>
          </div>
          <dl class="line-list">
            <div>
              <dt>创建</dt>
              <dd>{{ formatDateTime(order.createdAt) }}</dd>
            </div>
            <div>
              <dt>支付</dt>
              <dd>{{ formatDateTime(order.paidAt) || '—' }}</dd>
            </div>
            <div>
              <dt>接单</dt>
              <dd>{{ formatDateTime(order.acceptedAt) || '—' }}</dd>
            </div>
            <div>
              <dt>取物</dt>
              <dd>{{ formatDateTime(order.pickupAt) || '—' }}</dd>
            </div>
            <div>
              <dt>归还</dt>
              <dd>{{ formatDateTime(order.returnAt) || '—' }}</dd>
            </div>
            <div>
              <dt>完成</dt>
              <dd>{{ formatDateTime(order.completedAt) || '—' }}</dd>
            </div>
          </dl>
        </section>
      </div>

      <section class="panel">
        <div class="panel-title">
          <h2>订单留言</h2>
        </div>
        <p v-if="!messages.length" class="muted">暂无留言。</p>
        <div v-for="m in messages" :key="m.id" class="message">
          <header>
            {{ m.senderNickname || `#${m.senderId}` }} · {{ formatDateTime(m.createdAt) }}
          </header>
          {{ m.content }}
        </div>
        <div class="row" style="margin-top: 16px">
          <input
            v-model="newMessage"
            class="grow"
            type="text"
            maxlength="500"
            placeholder="输入留言内容，与对方沟通交接事宜"
            @keyup.enter="sendMessage"
          />
          <button type="button" class="btn primary" @click="sendMessage">发送</button>
        </div>
      </section>
    </template>
  </main>
</template>
