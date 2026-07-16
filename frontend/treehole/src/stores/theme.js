import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const THEME_KEY = 'treehole-theme'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref(localStorage.getItem(THEME_KEY) || 'system')

  function applyTheme(t) {
    let actualTheme = t
    if (t === 'system') {
      actualTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    document.documentElement.setAttribute('data-theme', actualTheme)
  }

  function setTheme(t) {
    theme.value = t
    localStorage.setItem(THEME_KEY, t)
    applyTheme(t)
  }

  // 监听系统主题变化
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (theme.value === 'system') {
        applyTheme('system')
      }
    })
  }

  // 初始化
  applyTheme(theme.value)

  return { theme, setTheme }
})
