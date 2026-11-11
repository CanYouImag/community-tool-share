<script setup>
import { getItem } from '@/api/modules/item';
import { formatMoney } from '@/utils/format';
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const item = ref(null);
const loading = ref(true);
const error = ref('');

watch(
    () => route.params.id,
    async (id, _oldId, onCleanup) => {
        let cancelled = false;
        onCleanup(() => {
            cancelled = true;
        });

        loading.value = true;
        error.value = '';
        item.value = null;

        try {
            const data = await getItem(id);
            if (!cancelled) {
                item.value = data;
            }
        } catch (err) {
            if (!cancelled) {
                error.value = err.message || '物品加载失败，请稍后重试';
            }
        } finally {
            if (!cancelled) {
                loading.value = false;
            }
        }
    },
    { immediate: true }
);
</script>

<template>
    <main class="public-main">
        <header class="page-head">
            <div>
                <h1>物品详情</h1>
            </div>
            <router-link class="btn" to="/items"> 返回物品广场 </router-link>
        </header>

        <section v-if="loading" class="panel" role="status">
            正在加载物品…
        </section>

        <section v-else-if="error" class="panel" role="alert">
            <h2>暂时无法查看物品</h2>
            <p>{{ error }}</p>
        </section>

        <div v-else-if="item" class="two">
            <section class="panel">
                <img
                    class="art large"
                    :src="item.image"
                    :alt="`${item.title}示意图`"
                />
            </section>

            <section class="panel">
                <h2>{{ item.title }}</h2>
                <p class="muted">
                    {{ item.category }} / {{ item.communityName }}
                </p>

                <hr class="rule" />

                <p class="price">
                    {{ formatMoney(item.dailyPrice) }}
                    <small>/ 天</small>
                </p>

                <dl class="line-list">
                    <div>
                        <dt>押金</dt>
                        <dd>{{ formatMoney(item.deposit) }}</dd>
                    </div>
                    <div>
                        <dt>出借人</dt>
                        <dd>{{ item.owner }}</dd>
                    </div>
                    <div>
                        <dt>信用分</dt>
                        <dd>{{ item.ownerCredit }}</dd>
                    </div>
                    <div>
                        <dt>所属社区</dt>
                        <dd>{{ item.communityName }}</dd>
                    </div>
                </dl>
            </section>
        </div>
    </main>
</template>
