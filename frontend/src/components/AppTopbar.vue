<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

// 顶部导航项，active 由路由匹配决定
const navItems = [
  { path: '/orders', label: '我的订单', match: ['orders'] },
  { path: '/community', label: '社区', match: ['community'] },
  { path: '/wallet', label: '钱包', match: ['wallet'] }
]

function isActive(item) {
  return item.match.some((seg) => route.path.includes(seg))
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
        <router-link to="/items" active-class="active">
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
        <router-link to="/account" active-class="active">
          个人中心
        </router-link>
      </nav>
    </div>
  </header>
</template>
