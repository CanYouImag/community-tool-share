<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAuthStore } from '@/store/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const loggingOut = ref(false);

async function logout() {
  if (loggingOut.value) return;
  loggingOut.value = true;
  try {
    // 先经过页面离开检查；取消放弃草稿时不退出账号。
    const failure = await router.push({ name: 'login' });
    if (failure && route.name !== 'login') return;
    await auth.logout();
  } finally {
    loggingOut.value = false;
  }
}

// 平台管理员可同时进「平台管理」与「社区管理」（平台管理员兼任社区 1 管理员时需审核加入申请）；
// 社区管理员仅进「社区管理」。
const adminNavItems = computed(() => {
  if (auth.user?.role === 'PLATFORM_ADMIN') {
    return [
      { to: '/admin/platform', label: '平台管理', admin: true },
      { to: '/admin/community', label: '社区管理', admin: true },
    ];
  }
  if (auth.user?.role === 'COMMUNITY_ADMIN') {
    return [{ to: '/admin/community', label: '社区管理', admin: true }];
  }
  return [];
});

// 详情、编辑等页面的导航归属统一在路由 meta.nav 中声明。
const navItems = computed(() => [
  { to: '/items', label: '物品广场' },
  { to: '/community', label: '社区' },
  { to: '/publish', label: '发布 / 我的物品' },
  { to: '/orders', label: '我的订单' },
  { to: '/wallet', label: '钱包' },
  auth.isLoggedIn ? { to: '/account', label: '个人中心' } : { to: '/login', label: '登录 / 注册' },
  ...adminNavItems.value,
]);
</script>

<template>
  <header class="top">
    <div class="top-inner">
      <router-link class="brand" to="/items">
        <span class="brand-symbol">邻</span>
        <span>邻租</span>
      </router-link>
      <nav class="nav" aria-label="主导航">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          :class="{ active: item.admin ? route.path === item.to : route.meta.nav === item.to }"
          :aria-current="route.meta.nav === item.to ? 'location' : undefined"
        >
          {{ item.label }}
        </router-link>
        <button
          v-if="auth.isLoggedIn"
          class="btn small"
          type="button"
          :disabled="loggingOut"
          @click="logout"
        >
          {{ loggingOut ? '正在退出…' : '退出登录' }}
        </button>
      </nav>
    </div>
  </header>
</template>
