<script setup>
import AppTopbar from './components/AppTopbar.vue';
import { useUserStore } from './store/user';
import { useAuthStore } from './store/auth';
import { computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const user = useUserStore();
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const requiresLogin = computed(() => route.meta.requiresAuth && !auth.isLoggedIn);

// 登录失效时也立即隐藏私有页面，不等下一次路由导航才拦截。
watch(
  requiresLogin,
  (blocked) => {
    if (blocked) router.replace({ name: 'login', query: { redirect: route.fullPath } });
  },
  { immediate: true },
);
</script>

<template>
  <AppTopbar />
  <!-- 切换账号或退出后重建页面，避免沿用上一账号的收藏、物品和表单。 -->
  <router-view v-if="!requiresLogin" :key="user.id ?? 'guest'" />
</template>
