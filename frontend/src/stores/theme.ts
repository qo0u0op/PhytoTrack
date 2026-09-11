import { computed, ref, watchEffect } from 'vue'
import { defineStore } from 'pinia'

export type Theme = 'light' | 'dark' | 'auto'

const STORAGE_KEY = 'phytotrack-theme'

function getSystemDark (): boolean {
  return typeof window !== 'undefined' && window.matchMedia ('(prefers-color-scheme: dark)').matches
}

function readStored (): Theme {
  const v = localStorage.getItem (STORAGE_KEY)
  if (v === 'light' || v === 'dark' || v === 'auto') return v
  return 'auto'
}

function applyTheme (effective: 'light' | 'dark') {
  document.documentElement.setAttribute ('data-bs-theme', effective)
}

export const useThemeStore = defineStore ('theme', () => {
  const theme = ref<Theme> (readStored ())
  const effectiveTheme = computed<'light' | 'dark'> (() => {
    if (theme.value !== 'auto') return theme.value
    return getSystemDark () ? 'dark' : 'light'
  })

  let media: MediaQueryList | null = null
  let mediaHandler: ((e: MediaQueryListEvent) => void) | null = null

  function init () {
    applyTheme (effectiveTheme.value)
    // 監聽系統偏好變更（僅 auto 時有效）
    if (typeof window !== 'undefined' && window.matchMedia) {
      media = window.matchMedia ('(prefers-color-scheme: dark)')
      mediaHandler = () => {
        if (theme.value === 'auto') applyTheme (effectiveTheme.value)
      }
      if (media.addEventListener) media.addEventListener ('change', mediaHandler)
      else (media as any).addListener (mediaHandler)
    }
    // 單一 watchEffect 同步 DOM 與持久化（涵蓋 theme 與 effectiveTheme）
    watchEffect (() => {
      localStorage.setItem (STORAGE_KEY, theme.value)
      applyTheme (effectiveTheme.value)
    })
  }

  function cycle () {
    theme.value = theme.value === 'auto' ? 'light' : theme.value === 'light' ? 'dark' : 'auto'
  }

  function setTheme (v: Theme) {
    theme.value = v
  }

  return { theme, effectiveTheme, init, cycle, setTheme }
})
