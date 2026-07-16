<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { authApi } from '@/api/auth'
import { hashPassword } from '@/utils/crypto'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toastStore = useToastStore()

const form = reactive({
  email: '',
  password: '',
})
const loading = ref(false)
const showPassword = ref(false)

async function handleLogin() {
  if (!form.email || !form.password) {
    toastStore.error('请填写邮箱和密码')
    return
  }
  loading.value = true
  try {
    const hashed = await hashPassword(form.password)
    const res = await authStore.login(form.email, hashed)
    if (res.code === 200) {
      toastStore.success('登录成功')
      const redirect = route.query.redirect || '/world'
      router.push(redirect)
    } else {
      toastStore.error(res.message || '登录失败')
    }
  } catch (e) {
    toastStore.error(e.message || '登录失败')
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
        <h1 class="auth-title">校园树洞</h1>
        <p class="auth-subtitle">欢迎回来，倾诉你的心声</p>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">邮箱</label>
          <input
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            autocomplete="email"
          />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <div class="password-input">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
              {{ showPassword ? '&#128065;' : '&#128064;' }}
            </button>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary w-full" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </div>

        <div class="auth-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
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
  max-width: 400px;
  box-shadow: var(--shadow-xl);
  animation: slideUp var(--transition-slow);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  font-size: 48px;
}

.auth-title {
  font-size: 28px;
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
  gap: 18px;
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

.password-input {
  position: relative;
}

.toggle-pwd {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: var(--text-tertiary);
  background: none;
}

.form-actions {
  margin-top: 8px;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}

.auth-footer a {
  font-weight: 500;
}
</style>
