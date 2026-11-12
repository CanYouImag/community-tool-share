<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getCaptcha } from '@/api/modules/auth';
import ViewTabs from '@/components/ViewTabs.vue';
import { useAuthStore } from '@/store/auth';

const tabs = [
  { id: 'login', label: '账号登录' },
  { id: 'register', label: '注册账号' },
];

const props = defineProps({
  initialTab: { type: String, default: 'login' },
});

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const activeTab = ref(props.initialTab === 'register' ? 'register' : 'login');
const submitting = ref(false);
const loginError = ref('');
const registerError = ref('');

const loginForm = reactive({
  account: '',
  password: '',
});

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
});

// 图形验证码由后端生成并校验，前端只持有 captchaId 和图片
const captchaId = ref('');
const captchaImage = ref('');
const captchaLoading = ref(false);
const captchaError = ref('');
const captchaInput = ref('');

async function refreshCaptcha() {
  captchaLoading.value = true;
  captchaError.value = '';
  captchaInput.value = '';
  try {
    const data = await getCaptcha();
    captchaId.value = data.captchaId;
    captchaImage.value = data.image;
  } catch {
    captchaId.value = '';
    captchaImage.value = '';
    captchaError.value = '验证码加载失败，请点击图片重试';
  } finally {
    captchaLoading.value = false;
  }
}

onMounted(refreshCaptcha);

function goAfterAuth() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '';
  if (redirect.startsWith('/')) {
    router.push(redirect);
    return;
  }
  router.push('/account');
}

async function submitLogin() {
  loginError.value = '';
  if (!loginForm.account.trim()) {
    loginError.value = '请输入用户名或手机号';
    return;
  }
  if (!loginForm.password) {
    loginError.value = '请输入密码';
    return;
  }
  if (!captchaInput.value.trim()) {
    loginError.value = '请输入图形验证码';
    return;
  }
  if (!captchaId.value) {
    loginError.value = '验证码未加载，请刷新后重试';
    refreshCaptcha();
    return;
  }

  submitting.value = true;
  try {
    await auth.login({
      account: loginForm.account.trim(),
      password: loginForm.password,
      captchaId: captchaId.value,
      captchaCode: captchaInput.value.trim(),
    });
    goAfterAuth();
  } catch (e) {
    loginError.value = e.message || '登录失败，请稍后重试';
    refreshCaptcha();
  } finally {
    submitting.value = false;
  }
}

async function submitRegister() {
  registerError.value = '';
  if (registerForm.username.trim().length < 3) {
    registerError.value = '用户名至少 3 位';
    return;
  }
  if (registerForm.password.length < 6) {
    registerError.value = '密码至少 6 位';
    return;
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    registerError.value = '两次输入的密码不一致';
    return;
  }

  submitting.value = true;
  try {
    await auth.register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.nickname.trim() || undefined,
    });
    goAfterAuth();
  } catch (e) {
    registerError.value = e.message || '注册失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <h1>登录/注册</h1>
    </header>

    <ViewTabs v-model="activeTab" :tabs="tabs" label="登录注册" />

    <section v-show="activeTab === 'login'" class="view-panel">
      <div class="narrow">
        <form class="panel" @submit.prevent="submitLogin">
          <h2>登录邻租</h2>
          <hr class="rule" />

          <label class="field">
            <span>用户名 / 手机号 <em>*</em></span>
            <input
              v-model.trim="loginForm.account"
              type="text"
              autocomplete="username"
              placeholder="请输入用户名或手机号"
            />
          </label>

          <label class="field">
            <span>密码 <em>*</em></span>
            <input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
          </label>

          <label class="field">
            <span>图形验证码 <em>*</em></span>
            <div class="captcha">
              <input
                v-model.trim="captchaInput"
                type="text"
                maxlength="4"
                autocomplete="off"
                placeholder="输入右侧字符"
              />
              <button
                class="captcha-code"
                type="button"
                aria-label="刷新验证码"
                :disabled="captchaLoading"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaImage"
                  :src="captchaImage"
                  alt="图形验证码，点击刷新"
                />
                <span v-else>{{ captchaLoading ? '加载中…' : '点击重试' }}</span>
              </button>
            </div>
            <small v-if="captchaError" class="field-hint error">{{ captchaError }}</small>
          </label>

          <p v-if="loginError" class="form-message error">{{ loginError }}</p>

          <div class="actions">
            <button class="btn primary" type="submit" :disabled="submitting">
              {{ submitting ? '登录中…' : '登录' }}
            </button>
            <button class="link-button" type="button" @click="activeTab = 'register'">
              注册新账号
            </button>
          </div>
        </form>
      </div>
    </section>

    <section v-show="activeTab === 'register'" class="view-panel">
      <div class="narrow">
        <form class="panel" @submit.prevent="submitRegister">
          <h2>创建邻租账号</h2>
          <hr class="rule" />

          <label class="field">
            <span>用户名 <em>*</em></span>
            <input
              v-model.trim="registerForm.username"
              type="text"
              autocomplete="username"
              placeholder="3-20 位中文、字母、数字或下划线"
            />
          </label>

          <label class="field">
            <span>密码 <em>*</em></span>
            <input
              v-model="registerForm.password"
              type="password"
              autocomplete="new-password"
              placeholder="至少 6 位"
            />
          </label>

          <label class="field">
            <span>确认密码 <em>*</em></span>
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="请再次输入密码"
            />
          </label>

          <label class="field">
            <span>昵称</span>
            <input
              v-model.trim="registerForm.nickname"
              type="text"
              maxlength="30"
              placeholder="选填，默认使用用户名"
            />
          </label>

          <p v-if="registerError" class="form-message error">{{ registerError }}</p>

          <div class="actions">
            <button class="btn primary" type="submit" :disabled="submitting">
              {{ submitting ? '创建中…' : '创建账号' }}
            </button>
            <button class="link-button" type="button" @click="activeTab = 'login'">
              已有账号，去登录
            </button>
          </div>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.form-message {
  margin: -4px 0 16px;
  font-size: 13px;
}

.form-message.error {
  color: var(--color-danger);
}

.captcha-code {
  cursor: pointer;
  user-select: none;
  width: 120px;
  height: 46px;
  padding: 0;
  overflow: hidden;
  font-size: 13px;
  letter-spacing: 0;
}

.captcha-code img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.field-hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
}

.field-hint.error {
  color: var(--color-danger);
}

.btn[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
