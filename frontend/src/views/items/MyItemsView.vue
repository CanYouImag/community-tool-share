<script setup>
import { getMyItems, updateItemStatus } from '@/api/modules/item';
import EmptyState from '@/components/EmptyState.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import { ITEM_STATUS } from '@/utils/constants';
import { formatMoney } from '@/utils/format';
import { onMounted, ref } from 'vue';

const items = ref([]);
const loading = ref(true);
const error = ref('');
const selectedStatus = ref('');
const savingId = ref('');
const actionError = ref('');

async function changeStatus(item) {
  if (savingId.value || loading.value) return;

  const nextStatus = item.status === 'AVAILABLE' ? 'PAUSED' : 'AVAILABLE';

  savingId.value = item.id;
  actionError.value = '';

  try {
    await updateItemStatus(item.id, nextStatus);
    await loadMyItems();
  } catch (err) {
    actionError.value = err.message || '操作失败，请重试';
  } finally {
    savingId.value = '';
  }
}

async function loadMyItems() {
  loading.value = true;
  error.value = '';

  try {
    const data = await getMyItems({
      status: selectedStatus.value,
    });
    items.value = data.records;
  } catch (err) {
    error.value = err.message || '我的物品加载失败，请重试';
  } finally {
    loading.value = false;
  }
}

onMounted(loadMyItems);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>我的物品</h1>
        <p>查看自己发布的物品及审核状态。</p>
      </div>
      <router-link class="btn primary" to="/publish"> 发布物品 </router-link>
    </header>

    <section class="panel">
      <div class="filters">
        <label class="field">
          <span>物品状态</span>
          <select
            v-model="selectedStatus"
            :disabled="loading || Boolean(savingId)"
            @change="
              actionError = '';
              loadMyItems();
            "
          >
            <option value="">全部状态</option>
            <option v-for="(state, value) in ITEM_STATUS" :key="value" :value="value">
              {{ state.label }}
            </option>
          </select>
        </label>
      </div>

      <p v-if="actionError" role="alert">{{ actionError }}</p>
      <p v-if="loading" role="status">正在加载我的物品…</p>

      <div v-else-if="error" role="alert">
        <p>{{ error }}</p>
        <button class="btn" type="button" @click="loadMyItems">重新加载</button>
      </div>

      <EmptyState
        v-else-if="items.length === 0"
        :text="selectedStatus ? '没有符合该状态的物品' : '你还没有发布物品'"
      />

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">物品</th>
              <th scope="col">所属社区</th>
              <th scope="col">日租金</th>
              <th scope="col">押金</th>
              <th scope="col">状态</th>
              <th scope="col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.title }}</td>
              <td>{{ item.communityName }}</td>
              <td>{{ formatMoney(item.dailyPrice) }}</td>
              <td>{{ formatMoney(item.deposit) }}</td>
              <td>
                <StatusBadge
                  :label="ITEM_STATUS[item.status]?.label || '未知状态'"
                  :cls="ITEM_STATUS[item.status]?.cls || 'neutral'"
                />
              </td>
              <td>
                <button
                  v-if="['AVAILABLE', 'PAUSED'].includes(item.status)"
                  class="btn small"
                  type="button"
                  :disabled="Boolean(savingId)"
                  @click="changeStatus(item)"
                >
                  {{
                    savingId === item.id
                      ? '处理中…'
                      : item.status === 'AVAILABLE'
                        ? '暂停出租'
                        : '恢复出租'
                  }}
                </button>
                <span v-else class="muted">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>
