<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { messageApi } from '@/api/user'
import Avatar from '@/components/common/Avatar.vue'
import { formatTime, formatDateTime, getRoleLabel } from '@/utils/format'

const route = useRoute()
const authStore = useAuthStore()
const toastStore = useToastStore()

const activeTab = ref('chat')
const conversations = ref([])
const chatUser = ref(null)
const messages = ref([])
const messageText = ref('')
const sending = ref(false)
const systemMessages = ref([])
const chatContainer = ref(null)

async function loadConversations() {
  try {
    const res = await messageApi.conversations()
    if (res.code === 200) {
      conversations.value = res.data
    }
  } catch {}
}

async function loadSystemMessages() {
  try {
    const res = await messageApi.systemMessages()
    if (res.code === 200) {
      systemMessages.value = res.data
    }
  } catch {}
}

async function openChat(userId) {
  try {
    // 获取对方用户信息（从conversations中找）
    const conv = conversations.value.find(c => c.userId === userId)
    if (conv) {
      chatUser.value = { id: userId, nickname: conv.nickname, avatar: conv.avatar, role: conv.role }
    }

    const res = await messageApi.chatHistory(userId)
    if (res.code === 200) {
      messages.value = res.data
      activeTab.value = 'chat'
      nextTick(() => {
        if (chatContainer.value) {
          chatContainer.value.scrollTop = chatContainer.value.scrollHeight
        }
      })
    }
  } catch {}
}

async function sendMessage() {
  if (!messageText.value.trim() || !chatUser.value) return
  sending.value = true
  try {
    const res = await messageApi.send({
      receiverId: chatUser.value.id,
      content: messageText.value.trim(),
    })
    if (res.code === 200) {
      messageText.value = ''
      await openChat(chatUser.value.id)
      await loadConversations()
    } else {
      toastStore.error(res.message || '发送失败')
    }
  } catch (e) {
    toastStore.error(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

async function readSystemMessage(id) {
  try {
    await messageApi.readSystemMessage(id)
    await loadSystemMessages()
  } catch {}
}

onMounted(async () => {
  await loadConversations()
  await loadSystemMessages()
  if (route.query.chat) {
    openChat(Number(route.query.chat))
  }
})
</script>

<template>
  <div class="messages-page">
    <div class="messages-container">
      <!-- 左侧列表 -->
      <div class="message-sidebar">
        <div class="sidebar-tabs">
          <button class="sidebar-tab" :class="{ active: activeTab === 'chat' }" @click="activeTab = 'chat'">
            私聊
          </button>
          <button class="sidebar-tab" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">
            系统消息
          </button>
        </div>

        <!-- 会话列表 -->
        <div v-if="activeTab === 'chat'" class="conv-list">
          <div v-if="conversations.length === 0" class="empty-sidebar">
            暂无会话
          </div>
          <div
            v-for="conv in conversations"
            :key="conv.userId"
            class="conv-item"
            :class="{ active: chatUser?.id === conv.userId }"
            @click="openChat(conv.userId)"
          >
            <Avatar :src="conv.avatar" :name="conv.nickname" :size="42" />
            <div class="conv-info">
              <div class="conv-header">
                <span class="conv-name">{{ conv.nickname }}</span>
                <span v-if="conv.unreadCount > 0" class="conv-badge">{{ conv.unreadCount }}</span>
              </div>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 系统消息列表 -->
        <div v-if="activeTab === 'system'" class="system-list">
          <div v-if="systemMessages.length === 0" class="empty-sidebar">
            暂无系统消息
          </div>
          <div
            v-for="msg in systemMessages"
            :key="msg.id"
            class="system-msg-item"
            :class="{ unread: !msg.isRead }"
            @click="readSystemMessage(msg.id)"
          >
            <div class="system-msg-icon">&#128227;</div>
            <div class="system-msg-body">
              <div class="system-msg-title">{{ msg.title || '系统通知' }}</div>
              <p class="system-msg-content">{{ msg.content }}</p>
              <span class="system-msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区 -->
      <div class="chat-area">
        <div v-if="!chatUser && activeTab === 'chat'" class="chat-empty">
          <div class="chat-empty-icon">&#128172;</div>
          <p>选择一个会话开始聊天</p>
        </div>

        <template v-else-if="chatUser && activeTab === 'chat'">
          <div class="chat-header">
            <Avatar :src="chatUser.avatar" :name="chatUser.nickname" :size="36" />
            <div>
              <span class="chat-name">{{ chatUser.nickname }}</span>
              <span class="chat-role text-tertiary">{{ getRoleLabel(chatUser.role) }}</span>
            </div>
          </div>

          <div ref="chatContainer" class="chat-messages">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="msg-bubble"
              :class="{ mine: msg.senderId === authStore.user?.id }"
            >
              <Avatar :src="msg.senderId === authStore.user?.id ? authStore.user.avatar : chatUser.avatar"
                      :name="msg.senderId === authStore.user?.id ? authStore.user.nickname : chatUser.nickname"
                      :size="32" />
              <div class="msg-content">
                <p class="msg-text">{{ msg.content }}</p>
                <span class="msg-time">{{ formatDateTime(msg.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <input
              v-model="messageText"
              placeholder="输入消息..."
              @keyup.enter="sendMessage"
            />
            <button class="btn btn-primary" :disabled="!messageText.trim() || sending" @click="sendMessage">
              发送
            </button>
          </div>
        </template>

        <div v-if="activeTab === 'system'" class="chat-empty">
          <div class="chat-empty-icon">&#128227;</div>
          <p>在左侧查看系统消息</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.messages-page {
  padding: 24px 0;
  height: calc(100vh - var(--header-height));
}

.messages-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  gap: 16px;
  height: 100%;
}

.message-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: var(--bg-surface);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color-light);
}

.sidebar-tab {
  flex: 1;
  padding: 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  border-bottom: 2px solid transparent;
}

.sidebar-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.conv-list, .system-list {
  flex: 1;
  overflow-y: auto;
}

.empty-sidebar {
  text-align: center;
  color: var(--text-tertiary);
  padding: 32px;
  font-size: 14px;
}

.conv-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.conv-item:hover {
  background: var(--bg-surface-hover);
}

.conv-item.active {
  background: var(--color-primary-light);
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-name {
  font-weight: 600;
  font-size: 14px;
}

.conv-badge {
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

.conv-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.system-msg-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
}

.system-msg-item.unread {
  background: var(--color-primary-light);
}

.system-msg-icon {
  font-size: 20px;
}

.system-msg-body {
  flex: 1;
}

.system-msg-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.system-msg-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.system-msg-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.chat-area {
  flex: 1;
  background: var(--bg-surface);
  border: 1px solid var(--border-color-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.chat-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color-light);
}

.chat-name {
  font-weight: 600;
}

.chat-role {
  font-size: 12px;
  margin-left: 6px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-bubble {
  display: flex;
  gap: 8px;
  max-width: 70%;
}

.msg-bubble.mine {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-content {
  display: flex;
  flex-direction: column;
}

.msg-text {
  padding: 10px 14px;
  background: var(--bg-surface-hover);
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.4;
}

.msg-bubble.mine .msg-text {
  background: var(--color-primary);
  color: #fff;
}

.msg-time {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.msg-bubble.mine .msg-time {
  text-align: right;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--border-color-light);
}

.chat-input input {
  flex: 1;
}

@media (max-width: 768px) {
  .message-sidebar {
    width: 200px;
  }
}
</style>
