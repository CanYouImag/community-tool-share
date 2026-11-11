import { defineStore } from 'pinia';
import { computed } from 'vue';

import { useAuthStore } from '@/store/auth';

// 物品模块沿用此接口，身份统一来自真实登录状态，不再维护预设用户。
export const useUserStore = defineStore('user', () => {
  const auth = useAuthStore();
  const isLoggedIn = computed(() => auth.isLoggedIn && auth.user?.id != null);
  const id = computed(() => (isLoggedIn.value ? auth.user.id : null));
  const name = computed(() => (isLoggedIn.value ? auth.displayName : '游客'));
  const role = computed(() => (isLoggedIn.value ? auth.user.role : 'GUEST'));
  const credit = computed(() => (isLoggedIn.value ? (auth.user.creditScore ?? 0) : 0));
  const isCertified = computed(() => isLoggedIn.value && auth.isCertified);

  return { id, name, role, credit, isLoggedIn, isCertified };
});
