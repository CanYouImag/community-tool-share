<script setup>
import { computed, onMounted, reactive, ref } from 'vue';

import { getWallet } from '@/api/modules/wallet';
import ViewTabs from '@/components/ViewTabs.vue';
import { useAuthStore } from '@/store/auth';
import { formatAmount } from '@/utils/format';

const auth = useAuthStore();

const activeTab = ref('profile');
const loading = ref(true);
const pageError = ref('');
const profileMessage = ref('');
const passwordMessage = ref('');
const realNameMessage = ref('');
const wallet = ref(null);

const profileForm = reactive({
  nickname: '',
  bio: '',
  avatarUrl: '',
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const realNameForm = reactive({
  realName: '',
  idCardNo: '',
});

const tabs = [
  { id: 'profile', label: '我的资料' },
  { id: 'settings', label: '编辑资料' },
  { id: 'verify', label: '实名认证' },
];

const realNameLabels = {
  UNVERIFIED: '未实名',
  PENDING: '审核中',
  VERIFIED: '已实名认证',
  REJECTED: '认证驳回',
};

const roleLabels = {
  REGISTERED: '注册用户',
  CERTIFIED: '认证用户',
  COMMUNITY_ADMIN: '社区管理员',
  PLATFORM_ADMIN: '平台管理员',
};

const user = computed(() => auth.user);
const avatarText = computed(() => (auth.displayName || '邻').slice(0, 1).toUpperCase());
const realNameText = computed(() => realNameLabels[user.value?.realNameStatus] || '未实名');
const roleText = computed(() => roleLabels[user.value?.role] || user.value?.role || '—');
const canSubmitRealName = computed(
  () => !['VERIFIED', 'PENDING'].includes(user.value?.realNameStatus),
);

function syncForms() {
  profileForm.nickname = user.value?.nickname || '';
  profileForm.bio = user.value?.bio || '';
  profileForm.avatarUrl = user.value?.avatarUrl || '';
  realNameForm.realName = user.value?.realName || '';
  realNameForm.idCardNo = '';
}

async function load() {
  pageError.value = '';
  if (!auth.isLoggedIn) {
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    await auth.loadMe();
    syncForms();
    try {
      wallet.value = await getWallet();
    } catch {
      wallet.value = null;
    }
  } catch (e) {
    pageError.value = e.message || '加载个人资料失败';
    if (e.message?.includes('登录')) {
      auth.clear();
    }
  } finally {
    loading.value = false;
  }
}

async function saveProfile() {
  profileMessage.value = '';
  try {
    await auth.updateProfile({
      nickname: profileForm.nickname,
      bio: profileForm.bio,
      avatarUrl: profileForm.avatarUrl,
    });
    syncForms();
    profileMessage.value = '资料已保存';
    activeTab.value = 'profile';
  } catch (e) {
    profileMessage.value = e.message || '保存失败';
  }
}

async function savePassword() {
  passwordMessage.value = '';
  if (passwordForm.newPassword.length < 6) {
    passwordMessage.value = '新密码至少 6 位';
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordMessage.value = '两次输入的新密码不一致';
    return;
  }
  try {
    await auth.changePassword({ ...passwordForm });
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordMessage.value = '密码已修改';
  } catch (e) {
    passwordMessage.value = e.message || '修改密码失败';
  }
}

async function submitRealName() {
  realNameMessage.value = '';
  if (!realNameForm.realName.trim()) {
    realNameMessage.value = '请输入真实姓名';
    return;
  }
  if (!/^\d{17}[\dXx]$/.test(realNameForm.idCardNo.trim())) {
    realNameMessage.value = '请输入 18 位身份证号';
    return;
  }
  try {
    await auth.submitRealName({
      realName: realNameForm.realName.trim(),
      idCardNo: realNameForm.idCardNo.trim(),
    });
    syncForms();
    realNameMessage.value = '实名认证已提交';
  } catch (e) {
    realNameMessage.value = e.message || '提交失败';
  }
}

onMounted(load);
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>个人中心</h1>
    </header>

    <div v-if="!auth.isLoggedIn" class="narrow">
      <section class="panel">
        <h2>还没有登录</h2>
        <p class="muted">登录后可以查看个人资料、实名认证和钱包信息。</p>
        <p v-if="pageError" class="form-message error">{{ pageError }}</p>
        <div class="actions">
          <router-link class="btn primary" to="/login">去登录</router-link>
          <router-link class="btn" to="/community">先逛逛社区</router-link>
        </div>
      </section>
    </div>

    <template v-else>
      <p v-if="pageError" class="form-message error">{{ pageError }}</p>

      <section v-if="loading" class="panel">正在加载个人资料…</section>

      <template v-else>
        <ViewTabs v-model="activeTab" :tabs="tabs" label="个人中心" />

        <section v-show="activeTab === 'profile'" class="view-panel">
          <section class="panel">
            <div class="row">
              <span class="avatar large">{{ avatarText }}</span>
              <div class="grow">
                <h2>{{ auth.displayName }}</h2>
                <small>@{{ user?.username }}</small>
              </div>
              <span class="badge" :class="user?.realNameStatus === 'VERIFIED' ? '' : 'warn'">
                {{ realNameText }}
              </span>
            </div>
          </section>

          <div class="stats">
            <div class="stat">
              <small>信用分</small>
              <div class="value">{{ user?.creditScore ?? '—' }}</div>
            </div>
            <div class="stat">
              <small>可用余额</small>
              <div class="value">{{ wallet ? formatAmount(wallet.balance) : '—' }}</div>
            </div>
            <div class="stat">
              <small>冻结金额</small>
              <div class="value">{{ wallet ? formatAmount(wallet.freezeBalance) : '—' }}</div>
            </div>
            <div class="stat">
              <small>累计收入</small>
              <div class="value">{{ wallet ? formatAmount(wallet.totalIncome) : '—' }}</div>
            </div>
          </div>

          <div class="two">
            <div>
              <section class="panel">
                <div class="panel-title">
                  <h2>常用功能</h2>
                </div>
                <div class="stack">
                  <div class="actions">
                    <router-link class="btn" to="/orders">我的订单</router-link>
                    <router-link class="btn" to="/wallet">我的钱包</router-link>
                    <router-link class="btn" to="/community">我的社区</router-link>
                  </div>
                  <div class="actions">
                    <router-link class="btn" to="/dispute">我的举报</router-link>
                    <button class="btn" type="button" @click="activeTab = 'settings'">
                      编辑资料
                    </button>
                  </div>
                </div>
              </section>
            </div>

            <div>
              <section class="panel">
                <div class="panel-title">
                  <h2>账户信息</h2>
                </div>
                <dl class="line-list">
                  <div>
                    <dt>账号</dt>
                    <dd>{{ user?.username }}</dd>
                  </div>
                  <div>
                    <dt>手机号</dt>
                    <dd>{{ user?.phone || '未绑定' }}</dd>
                  </div>
                  <div>
                    <dt>身份</dt>
                    <dd>{{ roleText }}</dd>
                  </div>
                  <div>
                    <dt>认证状态</dt>
                    <dd>{{ realNameText }}</dd>
                  </div>
                </dl>
                <div class="actions">
                  <button class="btn" type="button" @click="activeTab = 'settings'">
                    编辑资料
                  </button>
                  <button class="btn primary" type="button" @click="activeTab = 'verify'">
                    查看认证
                  </button>
                </div>
              </section>
            </div>
          </div>
        </section>

        <section v-show="activeTab === 'settings'" class="view-panel">
          <div class="two">
            <div>
              <form class="panel" @submit.prevent="saveProfile">
                <div class="panel-title">
                  <h2>公开资料</h2>
                </div>

                <div class="row">
                  <span class="avatar large">{{ avatarText }}</span>
                  <div class="grow"></div>
                </div>

                <hr class="rule" />

                <label class="field">
                  <span>昵称</span>
                  <input v-model.trim="profileForm.nickname" type="text" maxlength="30" />
                </label>

                <label class="field">
                  <span>头像地址</span>
                  <input
                    v-model.trim="profileForm.avatarUrl"
                    type="url"
                    placeholder="https://..."
                  />
                </label>

                <label class="field">
                  <span>个人简介</span>
                  <textarea v-model.trim="profileForm.bio" maxlength="255"></textarea>
                </label>

                <p v-if="profileMessage" class="form-message">{{ profileMessage }}</p>

                <div class="actions">
                  <button class="btn primary" type="submit">保存资料</button>
                  <button class="btn" type="button" @click="activeTab = 'profile'">取消</button>
                </div>
              </form>
            </div>

            <div>
              <section class="panel">
                <div class="panel-title">
                  <h2>账号与安全</h2>
                </div>
                <dl class="line-list">
                  <div>
                    <dt>账号</dt>
                    <dd>{{ user?.username }}</dd>
                  </div>
                  <div>
                    <dt>认证</dt>
                    <dd>{{ realNameText }}</dd>
                  </div>
                </dl>

                <form @submit.prevent="savePassword">
                  <label class="field">
                    <span>原密码</span>
                    <input
                      v-model="passwordForm.oldPassword"
                      type="password"
                      autocomplete="current-password"
                    />
                  </label>
                  <label class="field">
                    <span>新密码</span>
                    <input
                      v-model="passwordForm.newPassword"
                      type="password"
                      autocomplete="new-password"
                      placeholder="至少 6 位"
                    />
                  </label>
                  <label class="field">
                    <span>确认新密码</span>
                    <input
                      v-model="passwordForm.confirmPassword"
                      type="password"
                      autocomplete="new-password"
                    />
                  </label>

                  <p v-if="passwordMessage" class="form-message">{{ passwordMessage }}</p>

                  <div class="actions">
                    <button class="btn primary" type="submit">修改密码</button>
                  </div>
                </form>
              </section>
            </div>
          </div>
        </section>

        <section v-show="activeTab === 'verify'" class="view-panel">
          <div class="narrow">
            <section class="panel">
              <div class="panel-title">
                <h2>认证状态</h2>
                <span class="badge" :class="user?.realNameStatus === 'VERIFIED' ? '' : 'warn'">
                  {{ realNameText }}
                </span>
              </div>

              <dl class="line-list">
                <div>
                  <dt>真实姓名</dt>
                  <dd>{{ user?.realName || '—' }}</dd>
                </div>
                <div>
                  <dt>身份证号</dt>
                  <dd>{{ user?.idCardMasked || '—' }}</dd>
                </div>
              </dl>

              <p v-if="user?.realNameStatus === 'PENDING'" class="form-message">
                认证材料已提交，等待平台审核。
              </p>

              <form v-if="canSubmitRealName" @submit.prevent="submitRealName">
                <hr class="rule" />
                <label class="field">
                  <span>真实姓名 <em>*</em></span>
                  <input v-model.trim="realNameForm.realName" type="text" maxlength="20" />
                </label>
                <label class="field">
                  <span>身份证号 <em>*</em></span>
                  <input
                    v-model.trim="realNameForm.idCardNo"
                    type="text"
                    maxlength="18"
                    placeholder="18 位身份证号"
                  />
                </label>

                <p v-if="realNameMessage" class="form-message">{{ realNameMessage }}</p>

                <div class="actions">
                  <button class="btn primary" type="submit">提交认证申请</button>
                  <button class="btn" type="button" @click="activeTab = 'profile'">
                    返回个人中心
                  </button>
                </div>
              </form>

              <p v-else-if="user?.realNameStatus === 'VERIFIED'" class="form-message">
                已完成实名认证，可以发布和租用物品。
              </p>
            </section>
          </div>
        </section>
      </template>
    </template>
  </main>
</template>

<style scoped>
.form-message {
  margin: 12px 0;
  color: var(--color-accent);
  font-size: 13px;
}

.form-message.error {
  color: var(--color-danger);
}

.row > .grow small {
  display: block;
  margin-top: 2px;
}

@media (max-width: 760px) {
  .two {
    grid-template-columns: 1fr;
  }
}
</style>
