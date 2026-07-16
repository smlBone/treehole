<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useToastStore } from '@/stores/toast'
import Avatar from '@/components/common/Avatar.vue'

const props = defineProps({
  unreadCount: { type: Number, default: 0 },
})

const emit = defineEmits(['unread-updated'])

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const toastStore = useToastStore()

const showUserMenu = ref(false)
const showThemeMenu = ref(false)

const currentBoard = computed(() => {
  if (route.name === 'world') return 'world'
  if (route.name === 'secret') return 'secret'
  return ''
})

function handleLogout() {
  authStore.logout()
  showUserMenu.value = false
  toastStore.success('已退出登录')
  router.push('/login')
}

function toggleTheme() {
  showThemeMenu.value = !showThemeMenu.value
}

function setTheme(t) {
  themeStore.setTheme(t)
  showThemeMenu.value = false
}

const themeLabel = computed(() => {
  const labels = { light: '浅色', dark: '深色', system: '跟随系统' }
  return labels[themeStore.theme] || '跟随系统'
})

const themeIcon = computed(() => {
  const icons = { light: '&#9728;', dark: '&#9789;', system: '&#9881;' }
  return icons[themeStore.theme] || '&#9881;'
})
</script>

<template>
  <header class="app-header glass">
    <div class="header-inner">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <span class="logo-icon">&#127794;</span>
        <span class="logo-text">校园树洞</span>
      </router-link>

      <!-- 导航 -->
      <nav class="nav-tabs">
        <router-link to="/world" class="nav-tab" :class="{ active: currentBoard === 'world' }">
          <span class="tab-icon">&#127760;</span>
          <span>世界树洞</span>
        </router-link>
        <router-link to="/secret" class="nav-tab" :class="{ active: currentBoard === 'secret' }">
          <span class="tab-icon">&#128274;</span>
          <span>秘密树洞</span>
        </router-link>
      </nav>

      <!-- 右侧操作 -->
      <div class="header-actions">
        <!-- 主题切换 -->
        <div class="theme-switcher">
          <button class="icon-btn" @click="toggleTheme" title="切换主题">
            <span v-html="themeIcon"></span>
          </button>
          <div v-if="showThemeMenu" class="dropdown-menu" @click.stop>
            <button class="dropdown-item" @click="setTheme('light')">浅色</button>
            <button class="dropdown-item" @click="setTheme('dark')">深色</button>
            <button class="dropdown-item" @click="setTheme('system')">跟随系统</button>
          </div>
        </div>

        <template v-if="authStore.isLoggedIn">
          <!-- 发帖按钮 -->
          <router-link to="/create" class="btn btn-primary header-btn">
            <span>&#9999;</span> 发帖
          </router-link>

          <!-- 消息 -->
          <router-link to="/messages" class="icon-btn msg-btn" title="消息">
            <span>&#9993;</span>
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </router-link>

          <!-- 管理后台 -->
          <router-link v-if="authStore.isAdmin" to="/admin" class="icon-btn" title="管理后台">
            <span>&#9881;</span>
          </router-link>

          <!-- 用户菜单 -->
          <div class="user-menu">
            <button class="user-trigger" @click="showUserMenu = !showUserMenu">
              <Avatar :src="authStore.user?.avatar" :name="authStore.user?.nickname" :size="34" />
            </button>
            <div v-if="showUserMenu" class="dropdown-menu user-dropdown" @click.stop>
              <div class="user-info-block">
                <div class="user-name">{{ authStore.user?.nickname }}</div>
                <div class="user-email text-tertiary">{{ authStore.user?.email }}</div>
              </div>
              <div class="dropdown-divider"></div>
              <router-link :to="`/user/${authStore.user?.id}`" class="dropdown-item" @click="showUserMenu = false">
                个人主页
              </router-link>
              <router-link to="/settings" class="dropdown-item" @click="showUserMenu = false">
                设置
              </router-link>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item text-danger" @click="handleLogout">退出登录</button>
            </div>
          </div>
        </template>

        <template v-else>
          <router-link to="/login" class="btn btn-ghost">登录</router-link>
          <router-link to="/register" class="btn btn-primary">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--header-height);
  z-index: 1000;
  border-bottom: 1px solid var(--border-color);
}

.header-inner {
  max-width: var(--content-max-width);
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 var(--spacing-md);
  gap: var(--spacing-lg);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  flex-shrink: 0;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  background: linear-gradient(135deg, var(--color-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-tabs {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.nav-tab:hover {
  background: var(--bg-surface-hover);
  color: var(--text-primary);
}

.nav-tab.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.tab-icon {
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.icon-btn {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 18px;
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  background: var(--bg-surface-hover);
  color: var(--text-primary);
}

.badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-danger);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-btn {
  font-size: 13px;
  padding: 7px 14px;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  min-width: 180px;
  padding: 6px;
  z-index: 100;
  animation: scaleIn var(--transition-fast);
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-primary);
  text-align: left;
  transition: background var(--transition-fast);
}

.dropdown-item:hover {
  background: var(--bg-surface-hover);
}

.dropdown-divider {
  height: 1px;
  background: var(--border-color-light);
  margin: 4px 0;
}

.user-menu {
  position: relative;
}

.user-trigger {
  padding: 2px;
  border-radius: 50%;
  transition: transform var(--transition-fast);
}

.user-trigger:hover {
  transform: scale(1.05);
}

.user-dropdown {
  min-width: 220px;
}

.user-info-block {
  padding: 12px 14px;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
}

.user-email {
  font-size: 13px;
  margin-top: 2px;
}

.theme-switcher {
  position: relative;
}

@media (max-width: 768px) {
  .logo-text {
    display: none;
  }
  .nav-tab span:last-child {
    display: none;
  }
  .header-btn span:first-child {
    display: none;
  }
}
</style>
