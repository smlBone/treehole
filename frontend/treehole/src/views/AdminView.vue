<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { postApi, reportApi } from '@/api/post'
import { messageApi } from '@/api/user'
import Modal from '@/components/common/Modal.vue'
import Avatar from '@/components/common/Avatar.vue'
import { formatTime, getReportTypeLabel, truncate } from '@/utils/format'

const authStore = useAuthStore()
const toastStore = useToastStore()

const activeTab = ref('reports')
const reports = ref([])
const pendingPosts = ref([])
const loading = ref(false)

const showBroadcast = ref(false)
const broadcastForm = reactive({
  userId: null,
  title: '',
  content: '',
})

const showHandleReport = ref(false)
const handlingReport = ref(null)
const handleForm = reactive({
  status: 'APPROVED',
  penalty: 5,
})

async function loadReports() {
  loading.value = true
  try {
    const res = await reportApi.pending(1, 50)
    if (res.code === 200) {
      reports.value = res.data
    }
  } catch {} finally {
    loading.value = false
  }
}

async function loadPendingPosts() {
  loading.value = true
  try {
    const res = await postApi.pending(1, 50)
    if (res.code === 200) {
      pendingPosts.value = res.data.list || res.data
    }
  } catch {} finally {
    loading.value = false
  }
}

async function reviewPost(id, action) {
  try {
    const res = await postApi.review(id, action)
    if (res.code === 200) {
      toastStore.success(action === 'approve' ? '已通过' : '已拒绝')
      pendingPosts.value = pendingPosts.value.filter(p => p.id !== id)
    }
  } catch (e) {
    toastStore.error(e.message || '操作失败')
  }
}

function openHandleReport(report) {
  handlingReport.value = report
  handleForm.status = 'APPROVED'
  handleForm.penalty = 5
  showHandleReport.value = true
}

async function submitHandleReport() {
  if (!handlingReport.value) return
  try {
    const res = await reportApi.handle({
      reportId: handlingReport.value.id,
      status: handleForm.status,
      penalty: handleForm.status === 'APPROVED' ? handleForm.penalty : 0,
    })
    if (res.code === 200) {
      toastStore.success('处理成功')
      showHandleReport.value = false
      reports.value = reports.value.filter(r => r.id !== handlingReport.value.id)
    }
  } catch (e) {
    toastStore.error(e.message || '处理失败')
  }
}

async function sendBroadcast() {
  if (!broadcastForm.content.trim()) {
    toastStore.error('消息内容不能为空')
    return
  }
  try {
    const res = await messageApi.broadcast({
      userId: broadcastForm.userId || null,
      title: broadcastForm.title || '系统通知',
      content: broadcastForm.content,
    })
    if (res.code === 200) {
      toastStore.success('发送成功')
      showBroadcast.value = false
      broadcastForm.userId = null
      broadcastForm.title = ''
      broadcastForm.content = ''
    }
  } catch (e) {
    toastStore.error(e.message || '发送失败')
  }
}

onMounted(() => {
  loadReports()
  loadPendingPosts()
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-container">
      <h1 class="page-title">&#9881; 管理后台</h1>

      <div class="admin-tabs">
        <button class="admin-tab" :class="{ active: activeTab === 'reports' }" @click="activeTab = 'reports'; loadReports()">
          举报管理
          <span v-if="reports.length" class="tab-count">{{ reports.length }}</span>
        </button>
        <button class="admin-tab" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'; loadPendingPosts()">
          帖子审核
          <span v-if="pendingPosts.length" class="tab-count">{{ pendingPosts.length }}</span>
        </button>
        <button class="admin-tab" :class="{ active: activeTab === 'broadcast' }" @click="activeTab = 'broadcast'">
          系统消息
        </button>
      </div>

      <!-- 举报管理 -->
      <div v-if="activeTab === 'reports'" class="admin-content">
        <div v-if="loading" class="loading-container"><div class="spinner"></div></div>
        <div v-else-if="reports.length === 0" class="empty-state">
          <div class="icon">&#9989;</div>
          <p>暂无待处理的举报</p>
        </div>
        <div v-else class="report-list">
          <div v-for="report in reports" :key="report.id" class="report-card card">
            <div class="report-info">
              <div class="report-meta">
                <span class="report-type-tag">{{ getReportTypeLabel(report.reportType) }}</span>
                <span class="report-target">{{ report.targetType === 'POST' ? '帖子' : '评论' }}</span>
                <span class="report-time text-tertiary">{{ formatTime(report.createdAt) }}</span>
              </div>
              <div class="report-content">
                <p class="report-text">{{ truncate(report.targetContent, 150) }}</p>
                <p v-if="report.reason" class="report-reason">
                  <span class="text-tertiary">举报理由：</span>{{ report.reason }}
                </p>
              </div>
            </div>
            <button class="btn btn-primary" @click="openHandleReport(report)">处理</button>
          </div>
        </div>
      </div>

      <!-- 帖子审核 -->
      <div v-if="activeTab === 'posts'" class="admin-content">
        <div v-if="loading" class="loading-container"><div class="spinner"></div></div>
        <div v-else-if="pendingPosts.length === 0" class="empty-state">
          <div class="icon">&#9989;</div>
          <p>暂无待审核的帖子</p>
        </div>
        <div v-else class="pending-list">
          <div v-for="post in pendingPosts" :key="post.id" class="pending-card card">
            <div class="pending-meta">
              <Avatar :src="post.authorAvatar" :name="post.authorNickname" :size="32" />
              <span class="pending-author">{{ post.authorNickname }}</span>
              <span class="pending-board">{{ post.board === 'SECRET' ? '秘密树洞' : '世界树洞' }}</span>
              <span class="text-tertiary">{{ formatTime(post.createdAt) }}</span>
            </div>
            <p class="pending-content">{{ truncate(post.content, 200) }}</p>
            <div class="pending-actions">
              <button class="btn btn-primary" @click="reviewPost(post.id, 'approve')">&#10003; 通过</button>
              <button class="btn btn-danger" @click="reviewPost(post.id, 'reject')">&#10007; 拒绝</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 系统消息 -->
      <div v-if="activeTab === 'broadcast'" class="admin-content">
        <div class="broadcast-panel card">
          <h3 class="panel-title">发送系统消息</h3>
          <div class="form-group">
            <label class="form-label">接收用户ID（留空则发给所有人）</label>
            <input v-model="broadcastForm.userId" type="number" placeholder="用户ID，留空发给所有人" />
          </div>
          <div class="form-group">
            <label class="form-label">标题</label>
            <input v-model="broadcastForm.title" placeholder="消息标题" />
          </div>
          <div class="form-group">
            <label class="form-label">内容</label>
            <textarea v-model="broadcastForm.content" placeholder="消息内容" rows="4"></textarea>
          </div>
          <button class="btn btn-primary" @click="sendBroadcast">发送</button>
        </div>
      </div>

      <!-- 处理举报弹窗 -->
      <Modal :show="showHandleReport" title="处理举报" width="440px" @close="showHandleReport = false">
        <div v-if="handlingReport" class="handle-form">
          <div class="handle-info">
            <p><span class="text-tertiary">类型：</span>{{ getReportTypeLabel(handlingReport.reportType) }}</p>
            <p><span class="text-tertiary">内容：</span>{{ truncate(handlingReport.targetContent, 100) }}</p>
          </div>
          <div class="form-group">
            <label class="form-label">处理结果</label>
            <div class="handle-options">
              <button class="handle-option" :class="{ selected: handleForm.status === 'APPROVED' }" @click="handleForm.status = 'APPROVED'">
                举报成立
              </button>
              <button class="handle-option" :class="{ selected: handleForm.status === 'REJECTED' }" @click="handleForm.status = 'REJECTED'">
                举报驳回
              </button>
            </div>
          </div>
          <div v-if="handleForm.status === 'APPROVED'" class="form-group">
            <label class="form-label">扣除信誉分</label>
            <input v-model.number="handleForm.penalty" type="number" min="1" max="100" />
          </div>
          <div class="form-actions">
            <button class="btn btn-ghost" @click="showHandleReport = false">取消</button>
            <button class="btn btn-primary" @click="submitHandleReport">确认处理</button>
          </div>
        </div>
      </Modal>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  padding: 24px 0;
}

.admin-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
}

.admin-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.admin-tab {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  border-bottom: 2px solid transparent;
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-danger);
  color: #fff;
  font-size: 11px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.report-list, .pending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-card {
  padding: 16px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.report-info {
  flex: 1;
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.report-type-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
  border-radius: var(--radius-full);
  font-weight: 500;
}

.report-target {
  font-size: 12px;
  color: var(--text-secondary);
}

.report-text {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 4px;
}

.report-reason {
  font-size: 13px;
  color: var(--text-secondary);
}

.pending-card {
  padding: 16px;
}

.pending-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.pending-author {
  font-weight: 600;
  font-size: 14px;
}

.pending-board {
  font-size: 12px;
  padding: 2px 8px;
  background: var(--bg-surface-hover);
  border-radius: var(--radius-full);
  color: var(--text-secondary);
}

.pending-content {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 12px;
}

.pending-actions {
  display: flex;
  gap: 8px;
}

.broadcast-panel {
  padding: 24px;
}

.panel-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 18px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
}

.handle-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.handle-info {
  padding: 12px;
  background: var(--bg-surface-hover);
  border-radius: var(--radius-sm);
  font-size: 14px;
  line-height: 1.6;
}

.handle-options {
  display: flex;
  gap: 8px;
}

.handle-option {
  flex: 1;
  padding: 10px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
}

.handle-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
