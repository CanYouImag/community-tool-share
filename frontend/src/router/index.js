import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/community'
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('@/views/community/CommunityView.vue'),
    meta: { title: '社区' }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/orders/OrdersView.vue'),
    meta: { title: '我的订单' }
  },
  {
    path: '/order-detail/:id',
    name: 'order-detail',
    component: () => import('@/views/orders/OrderDetailView.vue'),
    meta: { title: '订单详情与操作' }
  },
  {
    path: '/wallet',
    name: 'wallet',
    component: () => import('@/views/wallet/WalletView.vue'),
    meta: { title: '钱包' }
  },
  {
    path: '/dispute',
    name: 'dispute',
    component: () => import('@/views/dispute/DisputeView.vue'),
    meta: { title: '纠纷与举报' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach((to) => {
  const base = '邻租'
  document.title = to.meta?.title ? `${to.meta.title} · ${base}` : base
})

export default router
