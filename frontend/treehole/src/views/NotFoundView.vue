<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <p class="nf-text">页面不存在或已被删除</p>
      <button class="btn btn-primary" @click="router.push('/world')">返回首页</button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nf-content {
  text-align: center;
}

.nf-code {
  font-size: 96px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--color-primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.nf-text {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 16px 0 24px;
}
</style>
