<script setup>
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { reportApi } from '@/api/post'
import { getReportTypeLabel } from '@/utils/format'
import Modal from './Modal.vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], required: true },
})

const emit = defineEmits(['close'])

const authStore = useAuthStore()
const toastStore = useToastStore()

const reportType = ref('')
const reason = ref('')
const submitting = ref(false)

const reportTypes = ['PORN', 'POLITICAL', 'CYBERBULLY', 'FRAUD', 'RUMOR', 'OTHER']

watch(() => props.show, (val) => {
  if (val) {
    reportType.value = ''
    reason.value = ''
  }
})

async function submit() {
  if (!authStore.isLoggedIn) {
    toastStore.info('请先登录')
    return
  }
  if (!reportType.value) {
    toastStore.error('请选择举报类型')
    return
  }
  if (reportType.value === 'OTHER' && !reason.value.trim()) {
    toastStore.error('举报类型为"其它"时必须填写举报理由')
    return
  }

  submitting.value = true
  try {
    const res = await reportApi.create({
      targetType: props.targetType,
      targetId: props.targetId,
      reportType: reportType.value,
      reason: reason.value || null,
    })
    if (res.code === 200) {
      toastStore.success('举报已提交')
      emit('close')
    } else {
      toastStore.error(res.message || '举报失败')
    }
  } catch (e) {
    toastStore.error(e.message || '举报失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Modal :show="show" title="举报内容" width="440px" @close="emit('close')">
    <div class="report-form">
      <div class="form-group">
        <label class="form-label">举报类型 <span class="required">*</span></label>
        <div class="type-grid">
          <button
            v-for="type in reportTypes"
            :key="type"
            class="type-option"
            :class="{ selected: reportType === type }"
            @click="reportType = type"
          >
            {{ getReportTypeLabel(type) }}
          </button>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">
          举报理由
          <span v-if="reportType === 'OTHER'" class="required"> *（必填）</span>
          <span v-else class="text-tertiary">（选填）</span>
        </label>
        <textarea
          v-model="reason"
          placeholder="请详细描述举报理由..."
          rows="4"
          maxlength="500"
        ></textarea>
      </div>

      <div class="form-actions">
        <button class="btn btn-ghost" @click="emit('close')">取消</button>
        <button class="btn btn-danger" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '提交举报' }}
        </button>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.report-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.required {
  color: var(--color-danger);
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.type-option {
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  text-align: center;
}

.type-option:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.type-option.selected {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
