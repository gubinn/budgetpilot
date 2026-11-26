import { onBeforeUnmount, onMounted } from 'vue'

/**
 * Keep an open report fresh without a full page reload.
 *
 * - Reloads when the tab/app becomes visible or focused again (e.g. after
 *   recording a transaction in another tab, on the phone, or via Telegram).
 * - Polls while the report is visible, so a transaction added elsewhere shows
 *   up within a minute even if this tab never lost focus.
 * - Deduplicates bursts (focus + visibilitychange) and skips while a request
 *   is already in flight.
 */
export function useReportRefresh(reload, {
  intervalMs = 60000,
  minGapMs = 5000,
  isBusy = () => false
} = {}) {
  let lastAuto = 0
  let timer = null

  function maybeReload() {
    if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
    if (isBusy()) return
    const now = Date.now()
    if (now - lastAuto < minGapMs) return
    lastAuto = now
    reload()
  }

  function onVisibilityChange() {
    if (document.visibilityState === 'visible') maybeReload()
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('focus', maybeReload)
    timer = window.setInterval(maybeReload, intervalMs)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener('focus', maybeReload)
    if (timer) {
      window.clearInterval(timer)
      timer = null
    }
  })
}