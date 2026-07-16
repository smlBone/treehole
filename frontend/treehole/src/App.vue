<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { messageApi } from '@/api/user'
import AppHeader from '@/components/layout/AppHeader.vue'
import ToastContainer from '@/components/common/ToastContainer.vue'

const route = useRoute()
const authStore = useAuthStore()
const toastStore = useToastStore()

const unreadCount = ref(0)

const showHeader = computed(() => {
  return !['login', 'register'].includes(route.name)
})

async function fetchUnreadCount() {
  if (!authStore.isLoggedIn) return
  try {
    const res = await messageApi.unreadCount()
    if (res.code === 200) {
      unreadCount.value = res.data
    }
  } catch {}
}

let pollTimer = null

onMounted(() => {
  if (authStore.token) {
    authStore.fetchUser()
  }
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
})
</script>

<template>
  <div id="app-root">
    <AppHeader v-if="showHeader" :unread-count="unreadCount" @unread-updated="fetchUnreadCount" />
    <main class="main-content" :class="{ 'no-header': !showHeader }">
      <router-view />
    </main>
    <ToastContainer />
  </div>
</template>

<style scoped>
.main-content {
  min-height: calc(100vh - var(--header-height));
  padding-top: var(--header-height);
}

.main-content.no-header {
  padding-top: 0;
}
</style>
