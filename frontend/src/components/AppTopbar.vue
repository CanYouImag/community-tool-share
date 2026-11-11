<script setup>
import { ref } from 'vue';
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

// 顶部导航项，active 由路由匹配决定
const navItems = [
  { path: '/orders', label: '我的订单', match: ['orders'] },
  { path: '/community', label: '社区', match: ['community'] },
  { path: '/wallet', label: '钱包', match: ['wallet'] },
];

function isActive(item) {
  return item.match.some((seg) => route.path.includes(seg));
}
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
          to="/items"
          :class="{ active: route.path === '/items' || route.path.startsWith('/items/') }"
        >
          物品广场
        </router-link>
        <router-link to="/community" class="" :class="{ active: isActive(navItems[1]) }">
          社区
        </router-link>
        <router-link
          to="/publish"
          :class="{
            active:
              route.path === '/publish' ||
              route.path.startsWith('/publish/') ||
              route.path === '/my-items',
          }"
        >
          发布 / 我的物品
        </router-link>
        <router-link to="/orders" class="" :class="{ active: isActive(navItems[0]) }">
          我的订单
        </router-link>
        <router-link to="/wallet" class="" :class="{ active: isActive(navItems[2]) }">
          钱包
        </router-link>
        <template v-if="auth.isLoggedIn">
          <router-link to="/account" active-class="active"> 个人中心 </router-link>
          <button class="btn small" type="button" :disabled="loggingOut" @click="logout">
            {{ loggingOut ? '正在退出…' : '退出登录' }}
          </button>
        </template>
        <router-link v-else to="/login" active-class="active"> 登录 / 注册 </router-link>
      </nav>
    </div>
  </header>
</template>
