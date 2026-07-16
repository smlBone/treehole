/**
 * 格式化工具
 */

export function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  if (y === now.getFullYear()) return `${m}-${d}`
  return `${y}-${m}-${d}`
}

export function formatDateTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

export function getRoleLabel(role) {
  const labels = {
    USER: '普通用户',
    SPECIAL: '小树灵',
    REVIEWER: '审核员',
    ADMIN: '管理员',
  }
  return labels[role] || '未知'
}

export function getRoleBadgeClass(role) {
  const classes = {
    USER: 'badge-user',
    SPECIAL: 'badge-special',
    REVIEWER: 'badge-reviewer',
    ADMIN: 'badge-admin',
  }
  return classes[role] || 'badge-user'
}

export function getReportTypeLabel(type) {
  const labels = {
    PORN: '色情低俗',
    POLITICAL: '涉政敏感',
    CYBERBULLY: '引战网暴',
    FRAUD: '涉嫌诈骗',
    RUMOR: '传播谣言',
    OTHER: '其它',
  }
  return labels[type] || type
}

export function getPostStatusLabel(status) {
  const labels = {
    PENDING: '审核中',
    PUBLISHED: '已发布',
    REJECTED: '已拒绝',
    DELETED: '已删除',
  }
  return labels[status] || status
}

export function getCreditColor(score) {
  if (score >= 90) return 'var(--color-success)'
  if (score >= 60) return 'var(--color-warning)'
  return 'var(--color-danger)'
}

export function truncate(text, maxLen = 100) {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}
