<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToastStore } from '@/stores/toast'
import { authApi } from '@/api/auth'
import { hashPassword } from '@/utils/crypto'

const router = useRouter()
const toastStore = useToastStore()

const form = reactive({
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  code: '',
})

const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

const canSendCode = computed(() => {
  return form.email && countdown.value === 0 && !sendingCode.value
})

async function sendCode() {
  if (!form.email) {
    toastStore.error('请输入邮箱')
    return
  }
  sendingCode.value = true
  try {
    const res = await authApi.sendCode(form.email, 'REGISTER')
    if (res.code === 200) {
      toastStore.success('验证码已发送到邮箱')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    } else {
      toastStore.error(res.message || '发送失败')
    }
  } catch (e) {
    toastStore.error(e.message || '发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  if (!form.email || !form.nickname || !form.password || !form.code) {
    toastStore.error('请填写所有必填项')
    return
  }
  if (form.password !== form.confirmPassword) {
    toastStore.error('两次输入的密码不一致')
    return
  }
  if (form.password.length < 6) {
    toastStore.error('密码长度至少6位')
    return
  }

  loading.value = true
  try {
    const hashed = await hashPassword(form.password)
    const res = await authApi.register({
      email: form.email,
      nickname: form.nickname,
      password: hashed,
      code: form.code,
    })
    if (res.code === 200) {
      toastStore.success('注册成功，请登录')
      router.push('/login')
    } else {
      toastStore.error(res.message || '注册失败')
    }
  } catch (e) {
    toastStore.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <span class="auth-logo">&#127794;</span>
        <h1 class="auth-title">加入校园树洞</h1>
        <p class="auth-subtitle">在这里，你可以自由地表达心声</p>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="form-group">
          <label class="form-label">邮箱 <span class="required">*</span></label>
          <input v-model="form.email" type="email" placeholder="请输入真实邮箱" />
        </div>

        <div class="form-group">
          <label class="form-label">昵称 <span class="required">*</span></label>
          <input v-model="form.nickname" type="text" placeholder="给自己取个昵称" maxlength="20" />
        </div>

        <div class="form-group">
          <label class="form-label">验证码 <span class="required">*</span></label>
          <div class="code-input">
            <input v-model="form.code" type="text" placeholder="请输入验证码" maxlength="6" />
            <button
              type="button"
              class="btn btn-outline code-btn"
              :disabled="!canSendCode"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">密码 <span class="required">*</span></label>
          <input v-model="form.password" type="password" placeholder="至少6位" autocomplete="new-password" />
        </div>

        <div class="form-group">
          <label class="form-label">确认密码 <span class="required">*</span></label>
          <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" autocomplete="new-password" />
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary w-full" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            {{ loading ? '注册中...' : '注 册' }}
          </button>
        </div>

        <div class="auth-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-card {
  background: var(--bg-surface);
  border-radius: var(--radius-xl);
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: var(--shadow-xl);
  animation: slideUp var(--transition-slow);
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.auth-logo {
  font-size: 48px;
}

.auth-title {
  font-size: 26px;
  font-weight: 700;
  margin-top: 8px;
  background: linear-gradient(135deg, var(--color-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
}

.required {
  color: var(--color-danger);
}

.code-input {
  display: flex;
  gap: 8px;
}

.code-input input {
  flex: 1;
}

.code-btn {
  white-space: nowrap;
  padding: 8px 14px;
  font-size: 13px;
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-actions {
  margin-top: 8px;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
