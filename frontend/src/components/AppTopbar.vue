<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const auth = useAuthStore()

// 顶部导航项，active 由路由匹配决定
const navItems = [
  { path: '/orders', label: '我的订单', match: ['orders'] },
  { path: '/community', label: '社区', match: ['community'] },
  { path: '/wallet', label: '钱包', match: ['wallet'] }
]

const isAdmin = computed(() =>
  ['COMMUNITY_ADMIN', 'PLATFORM_ADMIN'].includes(auth.user?.role)
)

// 平台管理员默认进平台管理，社区管理员进社区管理
const adminHome = computed(() =>
  auth.user?.role === 'PLATFORM_ADMIN' ? '/admin/platform' : '/admin/community'
)

function isActive(item) {
  return item.match.some((seg) => route.path.includes(seg))
}
</script>

<template>
  <header class="top">
    <div class="top-inner">
      <router-link class="brand" to="/community">
        <span class="brand-symbol">邻</span>
        <span>邻租</span>
      </router-link>
      <nav class="nav" aria-label="主导航">
        <router-link to="/community" class="" :class="{ active: isActive(navItems[1]) }">
          物品广场
        </router-link>
        <router-link to="/community" class="" :class="{ active: isActive(navItems[1]) }">
          社区
        </router-link>
        <router-link to="/community" class="">发布 / 我的物品</router-link>
        <router-link to="/orders" class="" :class="{ active: isActive(navItems[0]) }">
          我的订单
        </router-link>
        <router-link to="/wallet" class="" :class="{ active: isActive(navItems[2]) }">
          钱包
        </router-link>
        <router-link to="/community" class="">个人中心</router-link>
        <router-link v-if="isAdmin" :to="adminHome" class="" :class="{ active: route.path.includes('/admin') }">
          管理后台
        </router-link>
      </nav>
    </div>
  </header>
</template>
