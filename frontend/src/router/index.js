import { createRouter, createWebHashHistory } from 'vue-router';

import { authRoutes } from '@/router/auth.routes';
import { useAuthStore } from '@/store/auth';

const routes = [
  ...authRoutes,
  {
    path: '/',
    redirect: '/items',
  },
  {
    path: '/items',
    name: 'items',
    component: () => import('@/views/items/ItemsView.vue'),
    meta: { title: '物品广场', nav: '/items' },
  },
  {
    path: '/items/:id',
    name: 'item-detail',
    component: () => import('@/views/items/ItemDetailView.vue'),
    meta: { title: '物品详情', nav: '/items' },
  },
  {
    path: '/my-items',
    name: 'my-items',
    component: () => import('@/views/items/MyItemsView.vue'),
    meta: { title: '我的物品', nav: '/publish' },
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('@/views/community/CommunityView.vue'),
    meta: { title: '社区', nav: '/community' },
  },
  {
    path: '/publish',
    name: 'publish',
    component: () => import('@/views/items/PublishView.vue'),
    meta: { title: '发布物品', nav: '/publish' },
  },
  {
    path: '/publish/:id/edit',
    name: 'edit-item',
    component: () => import('@/views/items/PublishView.vue'),
    meta: { title: '编辑物品', nav: '/publish' },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/orders/OrdersView.vue'),
    meta: { title: '我的订单', nav: '/orders', requiresAuth: true },
  },
  {
    path: '/order-detail/:id',
    name: 'order-detail',
    component: () => import('@/views/orders/OrderDetailView.vue'),
    meta: { title: '订单详情与操作', nav: '/orders', requiresAuth: true },
  },
  {
    path: '/wallet',
    name: 'wallet',
    component: () => import('@/views/wallet/WalletView.vue'),
    meta: { title: '钱包', nav: '/wallet', requiresAuth: true },
  },
  {
    path: '/dispute',
    name: 'dispute',
    component: () => import('@/views/dispute/DisputeView.vue'),
    meta: { title: '纠纷与举报', nav: '/orders', requiresAuth: true },
  },
  {
    path: '/admin',
    redirect: '/admin/platform',
  },
  {
    path: '/admin/community',
    name: 'admin-community',
    component: () => import('@/views/admin/CommunityAdminView.vue'),
    meta: { title: '社区管理' },
  },
  {
    path: '/admin/mediation',
    name: 'admin-mediation',
    component: () => import('@/views/admin/MediationView.vue'),
    meta: { title: '调解工作台' },
  },
  {
    path: '/admin/platform',
    name: 'admin-platform',
    component: () => import('@/views/admin/PlatformAdminView.vue'),
    meta: { title: '平台管理' },
  },
  {
    path: '/admin/arbitration',
    name: 'admin-arbitration',
    component: () => import('@/views/admin/ArbitrationView.vue'),
    meta: { title: '仲裁中心' },
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !useAuthStore().isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
});

router.afterEach((to) => {
  const base = '邻租';
  document.title = to.meta?.title ? `${to.meta.title} · ${base}` : base;
});

export default router;
