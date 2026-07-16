<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { postApi, likeApi } from '@/api/post'
import Avatar from '@/components/common/Avatar.vue'
import ReportModal from '@/components/common/ReportModal.vue'
import { formatTime, getRoleBadgeClass, getRoleLabel } from '@/utils/format'

const props = defineProps({
  post: { type: Object, required: true },
})

const emit = defineEmits(['deleted'])

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const liking = ref(false)
const showReport = ref(false)
const imagePreview = ref(null)

async function handleLike() {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    router.push('/login')
    return
  }
  if (liking.value) return
  liking.value = true
  try {
    const res = await likeApi.toggle('POST', props.post.id)
    if (res.code === 200) {
      props.post.isLiked = res.data.liked
      props.post.likeCount = res.data.likeCount
    }
  } catch (e) {
    toastStore.error(e.message || '操作失败')
  } finally {
    liking.value = false
  }
}

async function handleShare() {
  const url = `${window.location.origin}/post/${props.post.id}`
  try {
    await navigator.clipboard.writeText(url)
    toastStore.success('链接已复制到剪贴板')
    postApi.share(props.post.id)
  } catch {
    toastStore.error('复制失败')
  }
}

async function handleDelete() {
  try {
    const res = await postApi.delete(props.post.id)
    if (res.code === 200) {
      toastStore.success('删除成功')
      emit('deleted', props.post.id)
    }
  } catch (e) {
    toastStore.error(e.message || '删除失败')
  }
}

function openDetail() {
  router.push(`/post/${props.post.id}`)
}

function openImage(img) {
  imagePreview.value = img
}
</script>

<template>
  <article class="post-card card" @click="openDetail">
    <div class="post-header">
      <div class="post-author" @click.stop>
        <router-link v-if="post.userId" :to="`/user/${post.userId}`" class="author-link">
          <Avatar :src="post.authorAvatar" :name="post.authorNickname" :size="42" />
          <div class="author-info">
            <span class="author-name">{{ post.authorNickname }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </router-link>
        <div v-else class="author-link anonymous">
          <Avatar :src="null" :name="post.authorNickname" :size="42" />
          <div class="author-info">
            <span class="author-name">{{ post.authorNickname }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
      </div>

      <span v-if="post.board === 'SECRET'" class="board-tag secret">
        &#128274; 秘密树洞
      </span>
      <span v-else class="board-tag world">
        &#127760; 世界树洞
      </span>
    </div>

    <div class="post-content" @click="openDetail">
      <p class="post-text">{{ post.content }}</p>

      <div v-if="post.images && post.images.length" class="post-images" :class="`img-count-${Math.min(post.images.length, 4)}`">
        <div
          v-for="(img, i) in post.images.slice(0, 9)"
          :key="i"
          class="post-img-wrap"
          @click.stop="openImage(img)"
        >
          <img :src="img" :alt="`图片${i+1}`" loading="lazy" />
        </div>
      </div>

      <!-- 小树灵回复 -->
      <div v-if="post.robotResponse" class="robot-response" @click.stop>
        <div class="robot-header">
          <span class="robot-avatar">&#129418;</span>
          <span class="robot-name">小树灵</span>
          <span class="robot-tag">AI暖心回复</span>
        </div>
        <p class="robot-text">{{ post.robotResponse }}</p>
      </div>
    </div>

    <div class="post-actions" @click.stop>
      <button class="action-btn" :class="{ active: post.isLiked }" :disabled="liking" @click="handleLike">
        <span class="action-icon">{{ post.isLiked ? '&#10084;' : '&#9825;' }}</span>
        <span>{{ post.likeCount }}</span>
      </button>
      <button class="action-btn" @click="openDetail">
        <span class="action-icon">&#128172;</span>
        <span>{{ post.commentCount }}</span>
      </button>
      <button class="action-btn" @click="handleShare">
        <span class="action-icon">&#128279;</span>
        <span>{{ post.shareCount || '转发' }}</span>
      </button>

      <div class="action-right">
        <button
          v-if="authStore.isLoggedIn && !post.isAuthor && !authStore.isAdmin && !post.hasReported"
          class="action-btn report-btn"
          @click="showReport = true"
        >
          &#9873; 举报
        </button>
        <button
          v-if="post.isAuthor || authStore.isAdmin"
          class="action-btn delete-btn"
          @click="handleDelete"
        >
          &#128465; 删除
        </button>
      </div>
    </div>

    <!-- 图片预览 -->
    <Teleport to="body">
      <div v-if="imagePreview" class="image-preview-overlay" @click="imagePreview = null">
        <img :src="imagePreview" class="preview-img" />
        <button class="preview-close" @click="imagePreview = null">&times;</button>
      </div>
    </Teleport>

    <ReportModal
      :show="showReport"
      target-type="POST"
      :target-id="post.id"
      @close="showReport = false"
    />
  </article>
</template>

<style scoped>
.post-card {
  padding: 20px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: box-shadow var(--transition-fast);
}

.post-card:hover {
  box-shadow: var(--shadow-md);
}

.post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.author-link {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 600;
  font-size: 15px;
}

.author-link:hover .author-name {
  color: var(--color-primary);
}

.post-time {
  font-size: 13px;
  color: var(--text-tertiary);
}

.board-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.board-tag.secret {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.board-tag.world {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.post-content {
  margin-bottom: 14px;
}

.post-text {
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.post-images.img-count-1 { grid-template-columns: 1fr; max-width: 400px; }
.post-images.img-count-2 { grid-template-columns: repeat(2, 1fr); }
.post-images.img-count-3 { grid-template-columns: repeat(3, 1fr); }
.post-images.img-count-4 { grid-template-columns: repeat(2, 1fr); }

.post-img-wrap {
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg-surface-hover);
}

.post-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: zoom-in;
  transition: transform var(--transition-fast);
}

.post-img-wrap img:hover {
  transform: scale(1.05);
}

.robot-response {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.05), rgba(99, 102, 241, 0.05));
  border: 1px solid rgba(168, 85, 247, 0.2);
  border-radius: var(--radius-md);
}

.robot-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.robot-avatar {
  font-size: 20px;
}

.robot-name {
  font-weight: 600;
  color: #a855f7;
}

.robot-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
  border-radius: var(--radius-full);
}

.robot-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color-light);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 13px;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: var(--bg-surface-hover);
  color: var(--text-primary);
}

.action-btn.active {
  color: var(--color-danger);
}

.action-icon {
  font-size: 16px;
}

.action-right {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.report-btn:hover {
  color: var(--color-warning);
}

.delete-btn:hover {
  color: var(--color-danger);
}

.image-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.9);
  z-index: 9500;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.preview-img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  border-radius: var(--radius-md);
}

.preview-close {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 32px;
  color: #fff;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
