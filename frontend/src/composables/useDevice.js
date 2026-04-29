import { ref, onUnmounted } from 'vue'

const isMobile = ref(window.innerWidth < 768)
let listenerCount = 0
let cleanup = null

function ensureListener() {
  if (cleanup) return
  const handler = () => {
    isMobile.value = window.innerWidth < 768
  }
  window.addEventListener('resize', handler)
  cleanup = () => window.removeEventListener('resize', handler)
}

export function useDevice() {
  listenerCount++
  ensureListener()

  onUnmounted(() => {
    listenerCount--
    if (listenerCount === 0 && cleanup) {
      cleanup()
      cleanup = null
    }
  })

  return { isMobile }
}
