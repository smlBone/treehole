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
    const res = await postApi.list('WORLD', page.value, size.value)
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
          <span class="title-icon">&#127760;</span>
          世界树洞
        </h1>
        <p class="feed-desc">在这里分享你的故事，让世界听到你的声音</p>
      </div>

      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <div class="icon">&#128221;</div>
        <p>还没有人发帖，来发第一条吧！</p>
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
