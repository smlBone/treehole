<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: { type: String, default: null },
  name: { type: String, default: '?' },
  size: { type: Number, default: 40 },
})

const initials = computed(() => {
  if (!props.name) return '?'
  return props.name.charAt(0).toUpperCase()
})

const colors = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#ef4444']
const colorIndex = computed(() => {
  let hash = 0
  for (let i = 0; i < (props.name || '').length; i++) {
    hash = props.name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
})
</script>

<template>
  <div
    class="avatar"
    :style="{
      width: size + 'px',
      height: size + 'px',
      fontSize: size * 0.4 + 'px',
    }"
  >
    <img v-if="src" :src="src" :alt="name" class="avatar-img" />
    <span v-else class="avatar-initial" :style="{ background: colorIndex }">
      {{ initials }}
    </span>
  </div>
</template>

<style scoped>
.avatar {
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-surface-hover);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initial {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
}
</style>
