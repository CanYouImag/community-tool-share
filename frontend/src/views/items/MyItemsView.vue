<script setup>
import { onMounted, ref } from 'vue';

import { deleteItem, getMyItems, shelfItem } from '@/api/modules/item';
import EmptyState from '@/components/EmptyState.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import { useUserStore } from '@/store/user';
import { ITEM_STATUS } from '@/utils/constants';
import { formatMoney } from '@/utils/format';

const user = useUserStore();
const items = ref([]);
const loading = ref(true);
const error = ref('');
const selectedStatus = ref('');
const savingId = ref('');
const actionError = ref('');

function checkManagePermission() {
  if (!user.isLoggedIn) {
    actionError.value = '请先登录';
    return false;
  }

  if (!user.isCertified) {
    actionError.value = '请先完成实名认证后再管理物品';
    return false;
  }

  return true;
}

async function changeStatus(item) {
  if (savingId.value || loading.value) return;
  if (!checkManagePermission()) return;

  const isListed = item.status === 'LISTED';

  savingId.value = item.id;
  actionError.value = '';

  try {
    await shelfItem(item.id, !isListed);
    await loadMyItems();
  } catch (err) {
    actionError.value = err.message || '操作失败，请重试';
  } finally {
    savingId.value = '';
  }
}

async function removeItem(item) {
  if (savingId.value || loading.value) return;
  if (!checkManagePermission()) return;

  const confirmed = window.confirm(`确定删除「${item.title}」吗？删除后无法恢复。`);

  if (!confirmed) return;

  savingId.value = item.id;
  actionError.value = '';

  try {
    await deleteItem(item.id);
    await loadMyItems();
  } catch (err) {
    actionError.value = err.message || '删除失败，请重试';
  } finally {
    savingId.value = '';
  }
}

function changeFilter() {
  actionError.value = '';
  loadMyItems();
}

async function loadMyItems() {
  if (!user.isLoggedIn) {
    items.value = [];
    loading.value = false;
    return;
  }

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
      <h1>我的物品</h1>
      <router-link v-if="user.isCertified" class="btn primary" to="/publish">
        发布物品
      </router-link>
    </header>

    <section v-if="!user.isLoggedIn" class="panel" role="alert">
      <h2>请先登录</h2>
      <p>登录后才能查看自己发布的物品。</p>
      <router-link class="btn" to="/items">返回物品广场</router-link>
    </section>

    <section v-else class="panel">
      <p v-if="!user.isCertified" role="status">
        当前仅可查看物品，请先完成实名认证后再发布或管理物品。
      </p>
      <div class="filters">
        <label class="field">
          <span>物品状态</span>
          <select
            v-model="selectedStatus"
            :disabled="loading || Boolean(savingId)"
            @change="changeFilter"
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
              <td>{{ formatMoney(item.rentPerDay) }}</td>
              <td>{{ formatMoney(item.deposit) }}</td>
              <td>
                <StatusBadge
                  :label="ITEM_STATUS[item.status]?.label || '未知状态'"
                  :cls="ITEM_STATUS[item.status]?.cls || 'neutral'"
                />
              </td>
              <td>
                <router-link
                  v-if="
                    user.isCertified &&
                    !savingId &&
                    ['PENDING_REVIEW', 'REJECTED', 'LISTED', 'DELISTED'].includes(item.status)
                  "
                  class="btn small"
                  :to="{ name: 'edit-item', params: { id: item.id } }"
                >
                  编辑
                </router-link>
                <button
                  v-if="user.isCertified && ['LISTED', 'DELISTED'].includes(item.status)"
                  class="btn small"
                  type="button"
                  :disabled="Boolean(savingId)"
                  @click="changeStatus(item)"
                >
                  {{
                    savingId === item.id
                      ? '处理中…'
                      : item.status === 'LISTED'
                        ? '下架'
                        : '重新上架'
                  }}
                </button>
                <button
                  v-if="user.isCertified"
                  class="btn small"
                  type="button"
                  :disabled="Boolean(savingId)"
                  @click="removeItem(item)"
                >
                  {{ savingId === item.id ? '处理中…' : '删除' }}
                </button>
                <span v-if="!user.isCertified" class="muted">仅可查看</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>
