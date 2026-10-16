import { onMounted, onUnmounted } from 'vue'

/** 当前页面可见时同步；串行请求，恢复焦点立即补查，离开页面停止。 */
export function useAutoRefresh(refresh: () => Promise<void>, interval = 5000) {
  let active = false
  let running = false
  let timer: ReturnType<typeof setTimeout> | undefined
  function schedule() {
    if (active) timer = setTimeout(() => { void tick() }, interval)
  }
  async function tick() {
    if (!active || running) return
    clearTimeout(timer)
    if (document.visibilityState !== 'hidden') {
      running = true
      try { await refresh() }
      catch { /* 页面负责展示同步错误；下次恢复后继续。 */ }
      finally { running = false }
    }
    schedule()
  }
  function resume() { if (document.visibilityState !== 'hidden') void tick() }
  onMounted(() => {
    active = true
    schedule()
    window.addEventListener('focus', resume)
    document.addEventListener('visibilitychange', resume)
  })
  onUnmounted(() => {
    active = false
    clearTimeout(timer)
    window.removeEventListener('focus', resume)
    document.removeEventListener('visibilitychange', resume)
  })
}
