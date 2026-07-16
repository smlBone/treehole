<script setup>
import { ref, onMounted } from 'vue'
import { postApi } from '@/api/post'
import PostCard from '@/components/post/PostCard.vue'

const posts = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)

async function loadPosts(reset = false) {
  if (reset) {
    page.value = 1
    posts.value = []
    hasMore.value = true
  }
  if (!hasMore.value || loading.value) return
  loading.value = true
  try {
    const res = await postApi.list('SECRET', page.value, size.value)
    if (res.code === 200) {
      const data = res.data
      posts.value.push(...data.list)
      hasMore.value = posts.value.length < data.total
      page.value++
    }
  } catch {} finally {
    loading.value = false
  }
}

function handleDeleted(id) {
  posts.value = posts.value.filter(p => p.id !== id)
}

onMounted(() => loadPosts(true))
</script>

<template>
  <div class="feed-page">
    <div class="feed-container">
      <div class="feed-header">
        <h1 class="feed-title">
          <span class="title-icon">&#128274;</span>
          秘密树洞
        </h1>
        <p class="feed-desc">在这里，你是完全匿名的。放心倾诉，无人知晓。</p>
        <div class="secret-notice">
          <span class="notice-icon">&#128276;</span>
          <span>在秘密树洞发帖时可以@小树灵寻求安慰，你的身份将被完全隐藏</span>
        </div>
      </div>

      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <div class="icon">&#128274;</div>
        <p>还没有秘密被分享...</p>
      </div>

      <div class="post-list">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          @deleted="handleDeleted"
        />
      </div>

      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
      </div>

      <div v-if="hasMore && !loading && posts.length > 0" class="load-more">
        <button class="btn btn-outline" @click="loadPosts()">加载更多</button>
      </div>

      <div v-if="!hasMore && posts.length > 0" class="no-more">
        没有更多了
      </div>
    </div>
  </div>
</template>

<style scoped>
.feed-page {
  padding: 24px 0;
}

.feed-container {
  max-width: var(--post-max-width);
  margin: 0 auto;
  padding: 0 16px;
}

.feed-header {
  margin-bottom: 24px;
}

.feed-title {
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 28px;
}

.feed-desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.secret-notice {
  margin-top: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.08), rgba(99, 102, 241, 0.08));
  border: 1px solid rgba(168, 85, 247, 0.15);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.notice-icon {
  font-size: 18px;
}

.load-more {
  text-align: center;
  padding: 20px;
}

.no-more {
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
  padding: 20px;
}
</style>
