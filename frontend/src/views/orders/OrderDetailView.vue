<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getOrder, getOrderMessages, postOrderMessage } from '@/api/modules/order';
import ViewTabs from '@/components/ViewTabs.vue';
import { formatMoney } from '@/utils/format';

const route = useRoute();

const order = ref(null);
const messages = ref([]);
const newMessage = ref('');

// 七阶段 Tab（对齐设计文档订单状态机，文案保留原型）
const stages = [
  { id: 'pending', label: '待确认' },
  { id: 'checkout', label: '确认支付' },
  { id: 'pickup', label: '待取物' },
  { id: 'renting', label: '租用中' },
  { id: 'return', label: '归还确认' },
  { id: 'inspect', label: '待验收' },
  { id: 'complete', label: '完成 / 评价' },
];
const activeStage = ref('pending');

const orderTotal = computed(() => (order.value ? order.value.rent + order.value.deposit : 0));

async function load() {
  order.value = await getOrder(route.params.id);
  messages.value = await getOrderMessages(route.params.id);
}

async function sendMessage() {
  const content = newMessage.value.trim();
  if (!content) return;
  await postOrderMessage(route.params.id, content);
  messages.value.push({
    id: `m_${Date.now()}`,
    from: 'Starling',
    mine: true,
    time: '刚刚',
    content,
  });
  newMessage.value = '';
}

// 阶段操作按钮（演示用：点击后切换阶段）
function setStage(id) {
  activeStage.value = id;
}

// 根据路由 hash 定位初始阶段
function initStageFromHash() {
  const h = route.hash.replace('#', '');
  if (stages.some((s) => s.id === h)) activeStage.value = h;
}

onMounted(() => {
  initStageFromHash();
  load();
});

watch(
  () => route.hash,
  () => initStageFromHash(),
);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>订单详情与操作</h1>
    </header>

    <ViewTabs v-model="activeStage" :tabs="stages" label="订单阶段" />

    <!-- 待确认 -->
    <section v-show="activeStage === 'pending'" class="view-panel">
      <section class="panel">
        <h2>待确认 / 租客 / 出租者操作</h2>
        <hr class="rule" />
        <p>等待出租者接单</p>
        <hr class="rule" />
        <div class="actions">
          <button type="button" class="btn primary" @click="setStage('pickup')">
            出租者：确认接单
          </button>
          <button type="button" class="btn" @click="setStage('pending')">租客：取消订单</button>
        </div>
        <details class="inline-note">
          <summary>取消 / 拒单确认</summary>
          <p>
            订单仍在待确认时可取消。确认后退还租金 ¥{{ order?.rent }}，解冻押金 ¥{{
              order?.deposit
            }}；接单后不可单方取消。
          </p>
        </details>
      </section>
    </section>

    <!-- 确认支付 -->
    <section v-show="activeStage === 'checkout'" class="view-panel">
      <section class="panel">
        <h2>确认租期与费用</h2>
        <hr class="rule" />
        <label class="field">
          <span>开始日期</span>
          <input type="date" :value="order?.startDate" />
        </label>
        <label class="field">
          <span>结束日期</span>
          <input type="date" :value="order?.endDate" />
        </label>
        <dl class="line-list">
          <div>
            <dt>租金（{{ order?.days }} 天）</dt>
            <dd>{{ formatMoney(order?.rent) }}</dd>
          </div>
          <div>
            <dt>押金</dt>
            <dd>{{ formatMoney(order?.deposit) }}</dd>
          </div>
          <div>
            <dt>可用余额</dt>
            <dd>¥500.00</dd>
          </div>
        </dl>
        <div class="actions">
          <button type="button" class="btn primary" @click="setStage('pending')">
            确认支付 {{ formatMoney(orderTotal) }}
          </button>
        </div>
      </section>
    </section>

    <!-- 待取物 -->
    <section v-show="activeStage === 'pickup'" class="view-panel">
      <section class="panel">
        <h2>取物核销</h2>
        <hr class="rule" />
        <div class="two equal">
          <div>
            <h3>租客出示取物码</h3>
            <div class="code">{{ order?.pickupCode || '826 419' }}</div>
          </div>
          <div>
            <label class="field">
              <span>出租者输入取物码</span>
              <input type="text" :value="(order?.pickupCode || '826419').replace(/\s/g, '')" />
            </label>
            <label class="field">
              <span>取物存证（至少 1 张）</span>
              <input type="file" accept="image/*" multiple />
            </label>
            <div class="actions">
              <button type="button" class="btn primary" @click="setStage('renting')">
                核销并完成交接
              </button>
            </div>
          </div>
        </div>
      </section>
    </section>

    <!-- 租用中 -->
    <section v-show="activeStage === 'renting'" class="view-panel">
      <section class="panel">
        <h2>租用中</h2>
        <hr class="rule" />
        <p>请于 {{ order?.endDate }} 归还物品，归还时上传清晰照片。</p>
        <hr class="rule" />
        <div class="actions">
          <button type="button" class="btn primary" @click="setStage('return')">发起归还</button>
          <router-link class="btn" :to="{ name: 'dispute', hash: '#new' }">发起纠纷</router-link>
        </div>
        <details class="inline-note">
          <summary>逾期时的提示</summary>
          <p>到期前 1 天提醒。逾期按 1.5 倍日租金逐日累计；超 3 天加强纠纷入口提示。</p>
        </details>
      </section>
    </section>

    <!-- 归还确认 -->
    <section v-show="activeStage === 'return'" class="view-panel">
      <section class="panel">
        <h2>归还与核验</h2>
        <hr class="rule" />
        <div class="two equal">
          <div>
            <h3>租客提交归还</h3>
            <label class="field">
              <span>归还存证（至少 1 张）</span>
              <input type="file" accept="image/*" multiple />
            </label>
          </div>
          <div>
            <h3>出租者确认归还</h3>
            <hr class="rule" />
            <div class="actions">
              <button type="button" class="btn primary" @click="setStage('inspect')">
                确认归还并验收
              </button>
              <router-link class="btn" :to="{ name: 'dispute', hash: '#new' }"
                >发现损坏</router-link
              >
            </div>
          </div>
        </div>
      </section>
    </section>

    <!-- 待验收 -->
    <section v-show="activeStage === 'inspect'" class="view-panel">
      <section class="panel">
        <h2>出租者验收物品</h2>
        <hr class="rule" />
        <p>出租者请核对归还物品的完好情况。验收正常将结算并解冻押金；发现异常可发起纠纷。</p>
        <hr class="rule" />
        <div class="actions">
          <button type="button" class="btn primary" @click="setStage('complete')">
            验收正常，结算完成
          </button>
          <router-link class="btn" :to="{ name: 'dispute', hash: '#new' }"
            >验收异常，发起纠纷</router-link
          >
        </div>
        <details class="inline-note">
          <summary>验收说明</summary>
          <p>
            验收正常 → 订单完成，租客押金解冻、出租收入到账（扣除 10% 平台佣金）；验收异常 →
            订单转「纠纷中」，押金继续冻结，进入社区调解 / 平台仲裁流程。
          </p>
        </details>
      </section>
    </section>

    <!-- 完成 / 评价 -->
    <section v-show="activeStage === 'complete'" class="view-panel">
      <section class="panel">
        <h2>订单已完成</h2>
        <hr class="rule" />
        <p>
          租客押金 ¥{{ order?.deposit }} 已解冻；出租收入 ¥{{
            ((order?.rent || 0) * 0.9).toFixed(0)
          }}，平台佣金 ¥{{ ((order?.rent || 0) * 0.1).toFixed(0) }}（10%）。
        </p>
        <hr class="rule" />
        <label class="field">
          <span>本次交易评分</span>
          <select>
            <option>5 星</option>
            <option>4 星</option>
            <option>3 星</option>
            <option>2 星</option>
            <option>1 星</option>
          </select>
        </label>
        <label class="field">
          <span>评价内容</span>
          <textarea>配件齐全，交接顺利。</textarea>
        </label>
        <div class="actions">
          <button type="button" class="btn primary" @click="setStage('complete')">提交评价</button>
          <router-link class="btn" :to="{ name: 'wallet' }">查看钱包</router-link>
        </div>
      </section>
    </section>

    <!-- 订单信息 + 费用明细 + 留言 -->
    <div class="two">
      <div>
        <section class="panel">
          <div class="panel-title">
            <h2>订单信息</h2>
          </div>
          <div class="order-product">
            <img class="art" src="/assets/item-30556edc.svg" alt="物品示意图" />
            <div>
              <h3>{{ order?.itemTitle }} / 双电池套装</h3>
              <p class="muted">{{ order?.communityName }} / 出租者 {{ order?.lender }}</p>
              <small
                >{{ order?.startDate?.slice(5) }} 日至 {{ order?.endDate?.slice(5) }} 日 / 租期
                {{ order?.days }} 天</small
              >
            </div>
          </div>
          <hr class="rule" />
          <dl class="line-list">
            <div>
              <dt>租客</dt>
              <dd>{{ order?.borrower }}</dd>
            </div>
            <div>
              <dt>出租者</dt>
              <dd>{{ order?.lender }}</dd>
            </div>
            <div>
              <dt>租期</dt>
              <dd>{{ order?.startDate }} 至 {{ order?.endDate }}</dd>
            </div>
            <div>
              <dt>社区</dt>
              <dd>{{ order?.communityName }}</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ order?.createdAt }}</dd>
            </div>
          </dl>
        </section>
      </div>
      <div>
        <section class="panel">
          <div class="panel-title">
            <h2>费用明细</h2>
          </div>
          <dl class="line-list">
            <div>
              <dt>日租金</dt>
              <dd>{{ formatMoney(order?.dailyPrice) }}</dd>
            </div>
            <div>
              <dt>租期</dt>
              <dd>{{ order?.days }} 天</dd>
            </div>
            <div>
              <dt>租金</dt>
              <dd>{{ formatMoney(order?.rent) }}</dd>
            </div>
            <div>
              <dt>订单押金</dt>
              <dd>{{ formatMoney(order?.deposit) }}</dd>
            </div>
          </dl>
          <div class="total">
            <span>合计支付</span>
            <span class="price">{{ formatMoney(orderTotal) }}</span>
          </div>
          <router-link :to="{ name: 'wallet', hash: '#wallet' }">查看资金流水</router-link>
        </section>
      </div>
    </div>

    <!-- 订单留言（UC-D9） -->
    <section class="panel">
      <div class="panel-title">
        <h2>订单留言</h2>
      </div>
      <div v-for="m in messages" :key="m.id" class="message" :class="{ mine: m.mine }">
        <header>{{ m.from }} · {{ m.time }}</header>
        {{ m.content }}
      </div>
      <div class="row" style="margin-top: 16px">
        <input
          v-model="newMessage"
          class="grow"
          type="text"
          placeholder="输入留言内容，与对方沟通交接事宜"
          @keyup.enter="sendMessage"
        />
        <button type="button" class="btn primary" @click="sendMessage">发送</button>
      </div>
    </section>
  </main>
</template>
