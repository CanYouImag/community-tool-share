<script setup>
import { getItems } from '@/api/modules/item';
import EmptyState from '@/components/EmptyState.vue';
import ItemCard from '@/components/ItemCard.vue';
import { onMounted, ref } from 'vue';

const items = ref([]);
const total = ref(0);
const loading = ref(true);
const error = ref('');

async function loadItems() {
    loading.value = true;
    error.value = '';

    try {
        const data = await getItems();
        items.value = data.records;
        total.value = data.total;
    } catch (err) {
        error.value = err.message || '物品加载失败，请稍后重试';
    } finally {
        loading.value = false;
    }
}

onMounted(loadItems);
</script>

<template>
    <main class="public-main">
        <div class="page-head">
            <div>
                <h1>物品广场</h1>
                <p>发现邻里好物，让闲置工具流动起来。</p>
            </div>
        </div>

        <section class="panel">
            <p v-if="loading" role="status">正在加载物品…</p>

            <div v-else-if="error" role="alert">
                <p>{{ error }}</p>
                <button class="btn" type="button" @click="loadItems">
                    重新加载
                </button>
            </div>

            <template v-else>
                <div class="panel-title">
                    <h2>浏览物品</h2>
                    <small>共 {{ total }} 件物品</small>
                </div>

                <EmptyState v-if="items.length === 0" text="暂无物品" />

                <div v-else class="item-grid">
                    <ItemCard
                        v-for="item in items"
                        :key="item.id"
                        :item="item"
                    />
                </div>
            </template>
        </section>
    </main>
</template>
