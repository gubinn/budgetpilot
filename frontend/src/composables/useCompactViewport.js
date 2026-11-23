import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 平板等中等宽度视口（<= breakpoint）返回 true。
 * 用于在桌面组件里隐藏次要表格列，避免 iPad 上列被压成竖排。
 */
export function useCompactViewport(breakpoint = 1024) {
  const isCompact = ref(
    typeof window !== 'undefined' ? window.innerWidth <= breakpoint : false
  )
  let mediaQuery = null

  const update = (event) => {
    isCompact.value = event.matches
  }

  onMounted(() => {
    mediaQuery = window.matchMedia(`(max-width: ${breakpoint}px)`)
    isCompact.value = mediaQuery.matches
    mediaQuery.addEventListener('change', update)
  })

  onBeforeUnmount(() => {
    mediaQuery?.removeEventListener('change', update)
  })

  return isCompact
}