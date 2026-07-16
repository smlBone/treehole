<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { userApi } from '@/api/user'
import { postApi } from '@/api/post'
import Avatar from '@/components/common/Avatar.vue'
import PostCard from '@/components/post/PostCard.vue'
import { formatTime, getRoleLabel, getRoleBadgeClass, getCreditColor } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const user = ref(null)
const posts = ref([])
const activeTab = ref('posts')
const loading = ref(true)
const history = ref([])

const userId = computed(() => route.params.id)
const isMe = computed(() => authStore.user?.id === Number(userId.value))
const isSpecial = computed(() => user.value?.role === 'SPECIAL')

async function loadProfile() {
  loading.value = true
  try {
    const res = await userApi.profile(userId.value)
    if (res.code === 200) {
      user.value = res.data
      if (!isSpecial.value || isMe.value || authStore.isAdmin) {
        await loadUserPosts()
      }
    }
  } catch {
    toastStore.error('用户不存在')
    router.push('/world')
  } finally {
    loading.value = false
  }
}

async function loadUserPosts() {
  try {
    const res = await postApi.userPosts(userId.value, 1, 20)
    if (res.code === 200) {
      posts.value = res.data.list
    }
  } catch {}
}

async function loadHistory() {
  if (!isMe.value) return
  try {
    const res = await postApi.history(1, 20)
    if (res.code === 200) {
      history.value = res.data.list
    }
  } catch {}
}

async function handleFollow() {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    router.push('/login')
    return
  }
  try {
    if (user.value.isFollowing) {
      await userApi.unfollow(userId.value)
      user.value.isFollowing = false
      toastStore.success('已取消关注')
    } else {
      await userApi.follow(userId.value)
      user.value.isFollowing = true
      toastStore.success('关注成功')
    }
  } catch (e) {
    toastStore.error(e.message || '操作失败')
  }
}

async function handleBlock() {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    return
  }
  try {
    if (user.value.isBlocked) {
      await userApi.unblock(userId.value)
      user.value.isBlocked = false
      toastStore.success('已取消拉黑')
    } else {
      await userApi.block(userId.value)
      user.value.isBlocked = true
      user.value.isFollowing = false
      toastStore.success('已拉黑')
    }
  } catch (e) {
    toastStore.error(e.message || '操作失败')
  }
}

function handleMessage() {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    return
  }
  router.push({ path: '/messages', query: { chat: userId.value } })
}

watch(activeTab, (tab) => {
  if (tab === 'history' && isMe.value && history.value.length === 0) {
    loadHistory()
  }
})

watch(userId, () => loadProfile(), { immediate: false })
onMounted(() => loadProfile())
</script>

<template>
  <div class="profile-page">
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
    </div>

    <template v-else-if="user">
      <div class="profile-container">
        <!-- 个人信息卡片 -->
        <div class="profile-header card">
          <div class="profile-banner"></div>
          <div class="profile-info">
            <div class="avatar-section">
              <Avatar :src="user.avatar" :name="user.nickname" :size="80" />
              <div class="user-details">
                <div class="user-name-row">
                  <h2 class="user-name">{{ user.nickname }}</h2>
                  <span class="role-badge" :class="getRoleBadgeClass(user.role)">
                    {{ getRoleLabel(user.role) }}
                  </span>
                </div>
                <p v-if="user.bio" class="user-bio">{{ user.bio }}</p>
                <p v-else class="user-bio text-tertiary">这个人很神秘，什么都没留下</p>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="profile-actions" v-if="!isMe">
              <button
                class="btn"
                :class="user.isFollowing ? 'btn-outline' : 'btn-primary'"
                @click="handleFollow"
              >
                {{ user.isFollowing ? '已关注' : '关注' }}
              </button>
              <button class="btn btn-outline" @click="handleMessage">
                私信
              </button>
              <button
                class="btn btn-outline"
                :class="{ 'btn-danger': user.isBlocked }"
                @click="handleBlock"
              >
                {{ user.isBlocked ? '已拉黑' : '拉黑' }}
              </button>
            </div>
            <div v-else class="profile-actions">
              <router-link to="/settings" class="btn btn-outline">编辑资料</router-link>
            </div>
          </div>

          <!-- 统计信息 -->
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ user.postCount || 0 }}</span>
              <span class="stat-label">帖子</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ user.followingCount || 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ user.followerCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div v-if="!isSpecial || isMe || authStore.isAdmin" class="stat-item">
              <span class="stat-value">{{ user.likeReceivedCount || 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div v-if="(isMe || authStore.isAdmin) && user.creditScore !== null" class="stat-item">
              <span class="stat-value" :style="{ color: getCreditColor(user.creditScore) }">
                {{ user.creditScore }}
              </span>
              <span class="stat-label">信誉分</span>
            </div>
          </div>
        </div>

        <!-- 小树灵特殊提示 -->
        <div v-if="isSpecial && !isMe" class="special-notice card">
          <span class="notice-icon">&#129418;</span>
          <p>我是小树灵，专门在秘密树洞中为你带来慰藉的暖心AI。如果你想和我聊天，去秘密树洞发帖时@我吧！</p>
        </div>

        <!-- 标签栏 -->
        <div v-if="!isSpecial || isMe || authStore.isAdmin" class="tab-bar">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'posts' }"
            @click="activeTab = 'posts'"
          >
            帖子
          </button>
          <button
            v-if="isMe"
            class="tab-btn"
            :class="{ active: activeTab === 'history' }"
            @click="activeTab = 'history'"
          >
            浏览记录
          </button>
        </div>

        <!-- 内容区 -->
        <div v-if="!isSpecial || isMe || authStore.isAdmin" class="tab-content">
          <div v-if="activeTab === 'posts'">
            <div v-if="posts.length === 0" class="empty-state">
              <div class="icon">&#128221;</div>
              <p>暂无帖子</p>
            </div>
            <PostCard
              v-for="p in posts"
              :key="p.id"
              :post="p"
            />
          </div>

          <div v-if="activeTab === 'history' && isMe">
            <div v-if="history.length === 0" class="empty-state">
              <div class="icon">&#128065;</div>
              <p>暂无浏览记录</p>
            </div>
            <PostCard
              v-for="p in history"
              :key="p.id"
              :post="p"
            />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 24px 0;
}

.profile-container {
  max-width: var(--post-max-width);
  margin: 0 auto;
  padding: 0 16px;
}

.profile-header {
  overflow: hidden;
  margin-bottom: 16px;
}

.profile-banner {
  height: 120px;
  background: linear-gradient(135deg, var(--color-primary), #8b5cf6);
}

.profile-info {
  padding: 0 20px 16px;
  margin-top: -40px;
  position: relative;
}

.avatar-section {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.avatar-section :deep(.avatar) {
  border: 4px solid var(--bg-surface);
}

.user-details {
  flex: 1;
  padding-bottom: 8px;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 20px;
  font-weight: 700;
}

.role-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.badge-user { background: var(--bg-surface-hover); color: var(--text-secondary); }
.badge-special { background: rgba(168, 85, 247, 0.1); color: #a855f7; }
.badge-reviewer { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.badge-admin { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

.user-bio {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.profile-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.profile-stats {
  display: flex;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color-light);
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.special-notice {
  padding: 20px;
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.05), rgba(99, 102, 241, 0.05));
}

.notice-icon {
  font-size: 32px;
}

.special-notice p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.tab-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.tab-btn {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  border-bottom: 2px solid transparent;
  transition: all var(--transition-fast);
}

.tab-btn:hover {
  color: var(--text-primary);
}

.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}
</style>
