<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { postApi } from '@/api/post'
import { fileApi } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const form = reactive({
  board: 'WORLD',
  content: '',
  images: [],
  callRobot: false,
})

const loading = ref(false)
const uploading = ref(false)

const canSubmit = computed(() => {
  return form.content.trim() && form.board && !loading.value
})

const isSecret = computed(() => form.board === 'SECRET')

async function handleUpload(event) {
  const files = Array.from(event.target.files)
  if (!files.length) return

  const remaining = 9 - form.images.length
  if (files.length > remaining) {
    toastStore.error(`最多上传9张图片，还能上传${remaining}张`)
  }

  uploading.value = true
  for (const file of files.slice(0, remaining)) {
    try {
      const res = await fileApi.upload(file)
      if (res.code === 200) {
        form.images.push(res.data)
      }
    } catch (e) {
      toastStore.error(`图片上传失败: ${e.message}`)
    }
  }
  uploading.value = false
  event.target.value = ''
}

function removeImage(index) {
  form.images.splice(index, 1)
}

async function handleSubmit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    const res = await postApi.create({
      board: form.board,
      content: form.content.trim(),
      images: form.images.length ? form.images : null,
      callRobot: isSecret.value && form.callRobot,
    })
    if (res.code === 200) {
      toastStore.success(res.message || '发帖成功')
      if (isSecret.value) {
        router.push('/secret')
      } else {
        router.push('/world')
      }
    } else {
      toastStore.error(res.message || '发帖失败')
    }
  } catch (e) {
    toastStore.error(e.message || '发帖失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="create-page">
    <div class="create-container">
      <h1 class="page-title">发布帖子</h1>

      <!-- 板块选择 -->
      <div class="form-section">
        <label class="section-label">选择板块</label>
        <div class="board-options">
          <button
            class="board-option"
            :class="{ active: form.board === 'WORLD' }"
            @click="form.board = 'WORLD'"
          >
            <span class="board-icon">&#127760;</span>
            <div class="board-info">
              <span class="board-name">世界树洞</span>
              <span class="board-desc">显示你的昵称和头像</span>
            </div>
          </button>
          <button
            class="board-option"
            :class="{ active: form.board === 'SECRET' }"
            @click="form.board = 'SECRET'"
          >
            <span class="board-icon">&#128274;</span>
            <div class="board-info">
              <span class="board-name">秘密树洞</span>
              <span class="board-desc">完全匿名，无人知晓</span>
            </div>
          </button>
        </div>
      </div>

      <!-- 内容输入 -->
      <div class="form-section">
        <label class="section-label">说点什么...</label>
        <textarea
          v-model="form.content"
          class="content-input"
          placeholder="此刻你想说什么？"
          rows="6"
          maxlength="2000"
        ></textarea>
        <div class="char-count">{{ form.content.length }}/2000</div>
      </div>

      <!-- 图片上传 -->
      <div class="form-section">
        <label class="section-label">图片（最多9张）</label>
        <div class="image-grid">
          <div v-for="(img, i) in form.images" :key="i" class="image-item">
            <img :src="img" alt="" />
            <button class="remove-img" @click="removeImage(i)">&times;</button>
          </div>
          <label v-if="form.images.length < 9" class="upload-btn">
            <input type="file" accept="image/*" multiple @change="handleUpload" />
            <span v-if="uploading" class="spinner"></span>
            <span v-else class="upload-icon">+</span>
            <span class="upload-text">{{ uploading ? '上传中' : '添加图片' }}</span>
          </label>
        </div>
      </div>

      <!-- 小树灵选项 -->
      <div v-if="isSecret" class="form-section">
        <label class="robot-toggle" :class="{ active: form.callRobot }">
          <input type="checkbox" v-model="form.callRobot" />
          <span class="toggle-content">
            <span class="toggle-icon">&#129418;</span>
            <span>
              <span class="toggle-title">@小树灵 求安慰</span>
              <span class="toggle-desc">让小树灵为你送上一份暖心的回复</span>
            </span>
          </span>
        </label>
      </div>

      <!-- 提交 -->
      <div class="form-actions">
        <button class="btn btn-ghost" @click="router.back()">取消</button>
        <button class="btn btn-primary" :disabled="!canSubmit" @click="handleSubmit">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.create-page {
  padding: 24px 0;
}

.create-container {
  max-width: var(--post-max-width);
  margin: 0 auto;
  padding: 0 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 24px;
}

.form-section {
  margin-bottom: 24px;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.board-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.board-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  text-align: left;
  transition: all var(--transition-fast);
}

.board-option:hover {
  border-color: var(--color-primary);
}

.board-option.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.board-icon {
  font-size: 28px;
}

.board-info {
  display: flex;
  flex-direction: column;
}

.board-name {
  font-weight: 600;
  font-size: 15px;
}

.board-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.content-input {
  font-size: 15px;
  line-height: 1.6;
  min-height: 120px;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  max-width: 400px;
}

.image-item {
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-img {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-btn {
  aspect-ratio: 1;
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  color: var(--text-tertiary);
  transition: all var(--transition-fast);
}

.upload-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.upload-btn input {
  display: none;
}

.upload-icon {
  font-size: 28px;
}

.upload-text {
  font-size: 12px;
}

.robot-toggle {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.robot-toggle.active {
  border-color: #a855f7;
  background: rgba(168, 85, 247, 0.05);
}

.robot-toggle input {
  width: auto;
  margin-right: 12px;
  accent-color: #a855f7;
}

.toggle-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toggle-icon {
  font-size: 24px;
}

.toggle-title {
  font-weight: 600;
  display: block;
}

.toggle-desc {
  font-size: 12px;
  color: var(--text-secondary);
  display: block;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color-light);
}
</style>
