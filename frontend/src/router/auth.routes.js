// 登录 / 注册 / 个人中心路由。
// 按当前约定，暂不修改 router/index.js；需要启用时在 routes 数组里展开：
//
// import { authRoutes } from './auth.routes'
// const routes = [
//   ...authRoutes,
//   { path: '/', redirect: '/community' },
//   ...
// ]
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
