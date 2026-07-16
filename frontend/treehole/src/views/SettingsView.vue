<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useToastStore } from '@/stores/toast'
import { userApi, fileApi } from '@/api/user'
import { hashPassword } from '@/utils/crypto'
import Avatar from '@/components/common/Avatar.vue'
import { getCreditColor } from '@/utils/format'

const authStore = useAuthStore()
const themeStore = useThemeStore()
const toastStore = useToastStore()

const activeTab = ref('profile')

const profileForm = reactive({
  nickname: '',
  avatar: '',
  bio: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const savingProfile = ref(false)
const savingPassword = ref(false)
const uploadingAvatar = ref(false)

onMounted(() => {
  if (authStore.user) {
    profileForm.nickname = authStore.user.nickname || ''
    profileForm.avatar = authStore.user.avatar || ''
    profileForm.bio = authStore.user.bio || ''
  }
})

async function handleAvatarUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    const res = await fileApi.upload(file)
    if (res.code === 200) {
      profileForm.avatar = res.data
    }
  } catch (e) {
    toastStore.error('头像上传失败')
  } finally {
    uploadingAvatar.value = false
    event.target.value = ''
  }
}

async function saveProfile() {
  if (!profileForm.nickname.trim()) {
    toastStore.error('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    const res = await userApi.updateProfile({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      bio: profileForm.bio,
    })
    if (res.code === 200) {
      toastStore.success('保存成功')
      await authStore.fetchUser()
    }
  } catch (e) {
    toastStore.error(e.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    toastStore.error('请填写完整')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toastStore.error('两次密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    toastStore.error('密码至少6位')
    return
  }
  savingPassword.value = true
  try {
    const oldHashed = await hashPassword(passwordForm.oldPassword)
    const newHashed = await hashPassword(passwordForm.newPassword)
    const res = await userApi.changePassword({
      oldPassword: oldHashed,
      newPassword: newHashed,
    })
    if (res.code === 200) {
      toastStore.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      toastStore.error(res.message || '修改失败')
    }
  } catch (e) {
    toastStore.error(e.message || '修改失败')
  } finally {
    savingPassword.value = false
  }
}

function setTheme(t) {
  themeStore.setTheme(t)
}

const themeOptions = [
  { value: 'system', label: '跟随系统', icon: '&#9881;' },
  { value: 'light', label: '浅色', icon: '&#9728;' },
  { value: 'dark', label: '深色', icon: '&#9789;' },
]
</script>

<template>
  <div class="settings-page">
    <div class="settings-container">
      <h1 class="page-title">设置</h1>

      <div class="settings-layout">
        <!-- 侧边导航 -->
        <div class="settings-nav">
          <button class="nav-item" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">
            个人信息
          </button>
          <button class="nav-item" :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">
            修改密码
          </button>
          <button class="nav-item" :class="{ active: activeTab === 'theme' }" @click="activeTab = 'theme'">
            主题外观
          </button>
        </div>

        <!-- 内容区 -->
        <div class="settings-content">
          <!-- 个人信息 -->
          <div v-if="activeTab === 'profile'" class="settings-panel card">
            <h2 class="panel-title">个人信息</h2>

            <div class="form-group">
              <label class="form-label">头像</label>
              <div class="avatar-upload">
                <Avatar :src="profileForm.avatar" :name="profileForm.nickname" :size="80" />
                <label class="btn btn-outline upload-label">
                  {{ uploadingAvatar ? '上传中...' : '更换头像' }}
                  <input type="file" accept="image/*" @change="handleAvatarUpload" :disabled="uploadingAvatar" />
                </label>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">昵称</label>
              <input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
            </div>

            <div class="form-group">
              <label class="form-label">个人简介</label>
              <textarea v-model="profileForm.bio" placeholder="介绍一下自己吧" rows="3" maxlength="200"></textarea>
            </div>

            <div class="form-actions">
              <button class="btn btn-primary" :disabled="savingProfile" @click="saveProfile">
                {{ savingProfile ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>

          <!-- 修改密码 -->
          <div v-if="activeTab === 'password'" class="settings-panel card">
            <h2 class="panel-title">修改密码</h2>

            <div class="form-group">
              <label class="form-label">当前密码</label>
              <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" />
            </div>

            <div class="form-group">
              <label class="form-label">新密码</label>
              <input v-model="passwordForm.newPassword" type="password" placeholder="至少6位" />
            </div>

            <div class="form-group">
              <label class="form-label">确认新密码</label>
              <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
            </div>

            <div class="form-actions">
              <button class="btn btn-primary" :disabled="savingPassword" @click="changePassword">
                {{ savingPassword ? '修改中...' : '修改密码' }}
              </button>
            </div>
          </div>

          <!-- 主题外观 -->
          <div v-if="activeTab === 'theme'" class="settings-panel card">
            <h2 class="panel-title">主题外观</h2>

            <p class="panel-desc">选择你喜欢的主题，随时可以切换</p>

            <div class="theme-options">
              <button
                v-for="opt in themeOptions"
                :key="opt.value"
                class="theme-option"
                :class="{ active: themeStore.theme === opt.value }"
                @click="setTheme(opt.value)"
              >
                <span class="theme-icon" v-html="opt.icon"></span>
                <span class="theme-label">{{ opt.label }}</span>
                <span v-if="themeStore.theme === opt.value" class="theme-check">&#10003;</span>
              </button>
            </div>

            <div v-if="authStore.user?.creditScore !== null && authStore.user?.creditScore !== undefined" class="credit-info">
              <span class="credit-label">当前信誉分</span>
              <span class="credit-value" :style="{ color: getCreditColor(authStore.user.creditScore) }">
                {{ authStore.user.creditScore }}
              </span>
              <span class="credit-desc text-tertiary">
                {{ authStore.user.creditScore >= 90 ? '可直接发帖' : authStore.user.creditScore >= 60 ? '发帖需审核' : '账号已封禁' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  padding: 24px 0;
}

.settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 24px;
}

.settings-layout {
  display: flex;
  gap: 16px;
}

.settings-nav {
  width: 180px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  padding: 12px 16px;
  text-align: left;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background: var(--bg-surface-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 500;
}

.settings-content {
  flex: 1;
}

.settings-panel {
  padding: 24px;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.panel-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.upload-label {
  position: relative;
  cursor: pointer;
}

.upload-label input {
  display: none;
}

.form-actions {
  margin-top: 24px;
}

.theme-options {
  display: flex;
  gap: 12px;
}

.theme-option {
  flex: 1;
  padding: 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
  transition: all var(--transition-fast);
}

.theme-option:hover {
  border-color: var(--color-primary);
}

.theme-option.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.theme-icon {
  font-size: 28px;
}

.theme-label {
  font-size: 14px;
  font-weight: 500;
}

.theme-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--color-primary);
  font-weight: 700;
}

.credit-info {
  margin-top: 24px;
  padding: 16px;
  background: var(--bg-surface-hover);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 12px;
}

.credit-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.credit-value {
  font-size: 24px;
  font-weight: 700;
}

.credit-desc {
  font-size: 13px;
  margin-left: auto;
}

@media (max-width: 640px) {
  .settings-layout {
    flex-direction: column;
  }
  .settings-nav {
    width: 100%;
    flex-direction: row;
  }
  .nav-item {
    flex: 1;
    text-align: center;
  }
}
</style>
