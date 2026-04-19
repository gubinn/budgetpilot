import { ref } from 'vue'

const isMobile = ref(window.innerWidth < 768)

window.addEventListener('resize', () => {
  isMobile.value = window.innerWidth < 768
})

export function useDevice() {
  return { isMobile }
}
