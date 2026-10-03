import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const colorMode = ref<'light' | 'dark'>('light')
  const globalLoadingCount = ref(0)

  const isLoading = computed(() => globalLoadingCount.value > 0)

  function startLoading() {
    globalLoadingCount.value++
  }

  function stopLoading() {
    if (globalLoadingCount.value > 0) {
      globalLoadingCount.value--
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    sidebarCollapsed,
    colorMode,
    globalLoadingCount,
    isLoading,
    startLoading,
    stopLoading,
    toggleSidebar,
  }
})
