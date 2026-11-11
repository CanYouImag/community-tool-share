// 登录 / 注册 / 个人中心路由。
// 已在 router/index.js 注册，requiresAuth 由全局守卫统一检查。
export const authRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录/注册' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/account',
    name: 'account',
    component: () => import('@/views/auth/AccountView.vue'),
    meta: { title: '个人中心', requiresAuth: true },
  },
];
