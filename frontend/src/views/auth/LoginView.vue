<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const props = defineProps({
  initialTab: { type: String, default: 'login' }
})

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const activeTab = ref(props.initialTab === 'register' ? 'register' : 'login')
const submitting = ref(false)
const loginError = ref('')
const registerError = ref('')

const loginForm = reactive({
  account: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

// 演示用前端验证码；接入后端图形验证码接口后替换这里
const captchaCode = ref('')
const captchaInput = ref('')

function refreshCaptcha() {
  const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'
  let code = ''
  for (let i = 0; i < 4; i += 1) {
    code += chars[Math.floor(Math.random() * chars.length)]
  }
  captchaCode.value = code
  captchaInput.value = ''
}

refreshCaptcha()

function goAfterAuth() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect.startsWith('/')) {
    router.push(redirect)
    return
  }
  router.push(router.hasRoute('account') ? '/account' : '/community')
}

async function submitLogin() {
  loginError.value = ''
  if (!loginForm.account.trim()) {
    loginError.value = '请输入用户名或手机号'
    return
  }
  if (!loginForm.password) {
    loginError.value = '请输入密码'
    return
  }
  if (captchaInput.value.toUpperCase() !== captchaCode.value) {
    loginError.value = '验证码不正确'
    refreshCaptcha()
    return
  }

  submitting.value = true
  try {
    await auth.login({
      account: loginForm.account.trim(),
      password: loginForm.password
    })
    goAfterAuth()
  } catch (e) {
    loginError.value = e.message || '登录失败，请稍后重试'
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  registerError.value = ''
  if (registerForm.username.trim().length < 3) {
    registerError.value = '用户名至少 3 位'
    return
  }
  if (registerForm.password.length < 6) {
    registerError.value = '密码至少 6 位'
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }

  submitting.value = true
  try {
    await auth.register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.nickname.trim() || undefined
    })
    goAfterAuth()
  } catch (e) {
    registerError.value = e.message || '注册失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="public-main">
    <header class="page-head">
      <div>
        <h1>登录/注册</h1>
      </div>
    </header>

    <nav class="view-tabs" role="tablist" aria-label="登录注册">
      <a
        class="view-tab"
        href="#login"
        role="tab"
        :aria-selected="activeTab === 'login'"
        @click.prevent="activeTab = 'login'"
      >
        账号登录
      </a>
      <a
        class="view-tab"
        href="#register"
        role="tab"
        :aria-selected="activeTab === 'register'"
        @click.prevent="activeTab = 'register'"
      >
        注册账号
      </a>
    </nav>

    <section v-show="activeTab === 'login'" class="view-panel" role="tabpanel">
      <h2 class="view-heading">账号登录</h2>
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
              <output title="点击刷新验证码" @click="refreshCaptcha">{{ captchaCode }}</output>
            </div>
          </label>

          <p v-if="loginError" class="form-message error">{{ loginError }}</p>

          <div class="actions">
            <button class="btn primary" type="submit" :disabled="submitting">
              {{ submitting ? '登录中…' : '登录' }}
            </button>
            <a href="#register" @click.prevent="activeTab = 'register'">注册新账号</a>
          </div>

          <p class="auth-hint">演示账号：Starling / 123456</p>
        </form>
      </div>
    </section>

    <section v-show="activeTab === 'register'" class="view-panel" role="tabpanel">
      <h2 class="view-heading">注册账号</h2>
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
            <a href="#login" @click.prevent="activeTab = 'login'">已有账号，去登录</a>
          </div>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-hint {
  margin-top: 18px;
  color: var(--color-muted);
  font-size: 12px;
}

.form-message {
  margin: -4px 0 16px;
  font-size: 13px;
}

.form-message.error {
  color: var(--color-danger);
}

.captcha output {
  cursor: pointer;
  user-select: none;
}

.btn[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
