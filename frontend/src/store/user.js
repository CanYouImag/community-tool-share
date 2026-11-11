import { defineStore } from 'pinia';

// 当前登录用户状态（Mock 阶段预设为认证用户 Starling）
export const useUserStore = defineStore('user', {
  state: () => ({
    id: 'u_1001',
    name: 'Starling',
    role: 'CERTIFIED', // GUEST / REGISTERED / CERTIFIED / COMMUNITY_ADMIN / PLATFORM_ADMIN
    credit: 88,
    communities: ['c_1001', 'c_1002'], // 已加入 / 待审核社区
  }),
  getters: {
    isLoggedIn: (state) => state.role !== 'GUEST',
    isCertified: (state) => ['CERTIFIED', 'COMMUNITY_ADMIN', 'PLATFORM_ADMIN'].includes(state.role),
  },
});
