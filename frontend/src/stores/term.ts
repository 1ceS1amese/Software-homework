import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTerms } from '@/api/base'
import type { TermItem } from '@/api/types'

export const useTermStore = defineStore('term', () => {
  const terms = ref<TermItem[]>([])
  const currentTermId = ref<number | null>(null)
  const loading = ref(false)

  const currentTerm = computed(() => {
    if (!currentTermId.value) return terms.value[0] || null
    return terms.value.find(t => t.id === currentTermId.value) || null
  })

  // isEnrollingNow: 判断当前时间是否在选课期内 (供前端灰显未到选课期)
  const isEnrollingNow = computed(() => {
    if (!currentTerm.value) return false
    const now = new Date().getTime()
    const start = new Date(currentTerm.value.enrollStart).getTime()
    const end = new Date(currentTerm.value.enrollEnd).getTime()
    return now >= start && now <= end
  })

  const isWithdrawAllowedNow = computed(() => {
    if (!currentTerm.value) return false
    const deadline = currentTerm.value.withdrawEnd || currentTerm.value.enrollEnd
    return !!deadline && Date.now() <= new Date(deadline).getTime()
  })

  async function fetchTerms() {
    loading.value = true
    try {
      const list = await getTerms()
      terms.value = list || []
      if (terms.value.length > 0 && !terms.value.some(t => t.id === currentTermId.value)) {
        // default select enrolling or running term, or first
        const active = terms.value.find(t => t.status === 'ENROLLING' || t.status === 'RUNNING')
        currentTermId.value = active ? active.id : terms.value[0].id
      } else if (terms.value.length === 0) {
        currentTermId.value = null
      }
    } catch {
      terms.value = []
      currentTermId.value = null
    } finally {
      loading.value = false
    }
  }

  function switchTerm(termId: number) {
    currentTermId.value = termId
  }

  return {
    terms,
    currentTermId,
    currentTerm,
    loading,
    isEnrollingNow,
    isWithdrawAllowedNow,
    fetchTerms,
    switchTerm,
  }
})
