<script setup>
import { getMyItems } from '@/api/modules/item';
import EmptyState from '@/components/EmptyState.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import { ITEM_STATUS } from '@/utils/constants';
import { formatMoney } from '@/utils/format';
import { onMounted, ref } from 'vue';

const items = ref([]);
const loading = ref(true);
const error = ref('');

async function loadMyItems() {
  loading.value = true;
  error.value = '';

  try {
    const data = await getMyItems();
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
      <p v-if="loading" role="status">正在加载我的物品…</p>

      <div v-else-if="error" role="alert">
        <p>{{ error }}</p>
        <button class="btn" type="button" @click="loadMyItems">重新加载</button>
      </div>

      <EmptyState v-else-if="items.length === 0" text="你还没有发布物品" />

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">物品</th>
              <th scope="col">所属社区</th>
              <th scope="col">日租金</th>
              <th scope="col">押金</th>
              <th scope="col">状态</th>
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
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>
