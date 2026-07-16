<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { postApi, commentApi, likeApi } from '@/api/post'
import PostCard from '@/components/post/PostCard.vue'
import Avatar from '@/components/common/Avatar.vue'
import ReportModal from '@/components/common/ReportModal.vue'
import { formatTime, getRoleLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const post = ref(null)
const comments = ref([])
const loading = ref(true)
const commentText = ref('')
const replyTo = ref(null)
const submitting = ref(false)
const showReport = ref(false)
const reportTarget = ref({ type: 'COMMENT', id: 0 })

const postId = computed(() => route.params.id)

async function loadPost() {
  loading.value = true
  try {
    const res = await postApi.detail(postId.value)
    if (res.code === 200) {
      post.value = res.data
    } else {
      toastStore.error(res.message || '帖子不存在')
      router.push('/world')
    }
  } catch {
    router.push('/world')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    const res = await commentApi.list(postId.value)
    if (res.code === 200) {
      comments.value = res.data
    }
  } catch {}
}

async function submitComment() {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    router.push('/login')
    return
  }
  if (!commentText.value.trim()) {
    toastStore.error('评论内容不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await commentApi.create({
      postId: postId.value,
      content: commentText.value.trim(),
      parentId: replyTo.value,
    })
    if (res.code === 200) {
      toastStore.success(res.message || '评论成功')
      commentText.value = ''
      replyTo.value = null
      await loadComments()
      if (post.value) {
        post.value.commentCount = (post.value.commentCount || 0) + 1
      }
    } else {
      toastStore.error(res.message || '评论失败')
    }
  } catch (e) {
    toastStore.error(e.message || '评论失败')
  } finally {
    submitting.value = false
  }
}

function setReply(comment) {
  replyTo.value = comment.id
  commentText.value = ''
}

function cancelReply() {
  replyTo.value = null
}

async function deleteComment(id) {
  try {
    const res = await commentApi.delete(id)
    if (res.code === 200) {
      toastStore.success('删除成功')
      comments.value = comments.value.filter(c => c.id !== id && c.parentId !== id)
      if (post.value) {
        post.value.commentCount = Math.max(0, (post.value.commentCount || 0) - 1)
      }
    }
  } catch (e) {
    toastStore.error(e.message || '删除失败')
  }
}

async function toggleCommentLike(comment) {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    return
  }
  try {
    const res = await likeApi.toggle('COMMENT', comment.id)
    if (res.code === 200) {
      comment.isLiked = res.data.liked
    }
  } catch (e) {
    toastStore.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadPost()
  loadComments()
})
</script>

<template>
  <div class="detail-page">
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
    </div>

    <template v-else-if="post">
      <div class="detail-container">
        <!-- 帖子内容 -->
        <PostCard :post="post" @deleted="router.push(post.board === 'SECRET' ? '/secret' : '/world')" />

        <!-- 小树灵回复（详情页独立展示） -->
        <div v-if="post.robotResponse" class="robot-detail-response card">
          <div class="robot-avatar-large">&#129418;</div>
          <div class="robot-content">
            <div class="robot-name-row">
              <span class="robot-name">小树灵</span>
              <span class="robot-badge">AI暖心回复</span>
            </div>
            <p class="robot-message">{{ post.robotResponse }}</p>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="comments-section card">
          <h3 class="comments-title">
            评论 <span v-if="post.commentCount" class="comment-count">({{ post.commentCount }})</span>
          </h3>

          <!-- 评论输入 -->
          <div class="comment-input-area">
            <div v-if="replyTo" class="reply-indicator">
              <span>回复评论</span>
              <button @click="cancelReply">&times;</button>
            </div>
            <div class="comment-input-row">
              <Avatar v-if="authStore.user" :src="authStore.user.avatar" :name="authStore.user.nickname" :size="36" />
              <div v-else class="anon-avatar">?</div>
              <input
                v-model="commentText"
                placeholder="写下你的评论..."
                @keyup.enter="submitComment"
              />
              <button class="btn btn-primary" :disabled="!commentText.trim() || submitting" @click="submitComment">
                {{ submitting ? '...' : '发送' }}
              </button>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length === 0" class="empty-comments">
            还没有评论，来说点什么吧
          </div>

          <div v-else class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <Avatar :src="comment.authorAvatar" :name="comment.authorNickname" :size="36" />
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.authorNickname }}</span>
                  <span v-if="comment.isPostAuthor" class="author-tag">作者</span>
                  <span v-if="comment.authorRole === 'SPECIAL'" class="special-tag">小树灵</span>
                  <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <p class="comment-text">
                  <span v-if="comment.replyToNickname" class="reply-to">@{{ comment.replyToNickname }} </span>
                  {{ comment.content }}
                </p>
                <div class="comment-actions">
                  <button
                    class="comment-action"
                    :class="{ liked: comment.isLiked }"
                    @click="toggleCommentLike(comment)"
                  >
                    {{ comment.isLiked ? '&#10084;' : '&#9825;' }}
                  </button>
                  <button v-if="authStore.isLoggedIn" class="comment-action" @click="setReply(comment)">
                    回复
                  </button>
                  <button
                    v-if="authStore.isLoggedIn && !comment.isPostAuthor"
                    class="comment-action report-action"
                    @click="reportTarget = { type: 'COMMENT', id: comment.id }; showReport = true"
                  >
                    举报
                  </button>
                  <button
                    v-if="comment.userId === authStore.user?.id || authStore.isAdmin || post.isAuthor"
                    class="comment-action delete-action"
                    @click="deleteComment(comment.id)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <ReportModal
      :show="showReport"
      :target-type="reportTarget.type"
      :target-id="reportTarget.id"
      @close="showReport = false"
    />
  </div>
</template>

<style scoped>
.detail-page {
  padding: 24px 0;
}

.detail-container {
  max-width: var(--post-max-width);
  margin: 0 auto;
  padding: 0 16px;
}

.robot-detail-response {
  display: flex;
  gap: 16px;
  padding: 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.05), rgba(99, 102, 241, 0.05));
  border: 1px solid rgba(168, 85, 247, 0.2);
}

.robot-avatar-large {
  font-size: 40px;
  flex-shrink: 0;
}

.robot-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.robot-name {
  font-weight: 700;
  color: #a855f7;
}

.robot-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
  border-radius: var(--radius-full);
}

.robot-message {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

.comments-section {
  padding: 20px;
}

.comments-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-count {
  color: var(--text-tertiary);
  font-size: 14px;
  font-weight: 400;
}

.comment-input-area {
  margin-bottom: 20px;
}

.reply-indicator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-surface-hover);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.comment-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.anon-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--bg-surface-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.comment-input-row input {
  flex: 1;
}

.empty-comments {
  text-align: center;
  color: var(--text-tertiary);
  padding: 32px;
  font-size: 14px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
}

.author-tag, .special-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.author-tag {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.special-tag {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.comment-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.comment-text {
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
}

.reply-to {
  color: var(--color-primary);
  font-weight: 500;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.comment-action {
  font-size: 13px;
  color: var(--text-tertiary);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.comment-action:hover {
  background: var(--bg-surface-hover);
  color: var(--text-primary);
}

.comment-action.liked {
  color: var(--color-danger);
}

.delete-action:hover {
  color: var(--color-danger);
}

.report-action:hover {
  color: var(--color-warning);
}
</style>
