import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMyEnrollments, enrollClass, withdrawClass } from '@/api/enrollments'
import type { EnrollmentItem, TeachingClassItem } from '@/api/types'

export const useEnrollStore = defineStore('enroll', () => {
  const myEnrollments = ref<EnrollmentItem[]>([])
  const loading = ref(false)
  let latestFetch = 0

  const activeEnrollments = computed(() => {
    return myEnrollments.value.filter(e => e.status === 'ENROLLED')
  })

  const totalCredits = computed(() => {
    return activeEnrollments.value.reduce((acc, curr) => acc + (curr.credit || 0), 0)
  })

  // 已选班级 ID 集合
  const enrolledClassIds = computed(() => {
    return new Set(activeEnrollments.value.map(e => e.teachingClassId))
  })

  async function fetchMine(termId?: number) {
    const requestId = ++latestFetch
    loading.value = true
    try {
      const res = await getMyEnrollments(termId)
      if (requestId !== latestFetch) return
      myEnrollments.value = res || []
    } catch (error) {
      if (requestId !== latestFetch) return
      myEnrollments.value = []
      throw error
    } finally {
      if (requestId === latestFetch) loading.value = false
    }
  }

  async function add(teachingClass: TeachingClassItem) {
    await enrollClass(teachingClass.id)
    try { await fetchMine(teachingClass.termId) } catch { /* 页面会提示同步失败 */ }
  }

  async function remove(teachingClassId: number) {
    await withdrawClass(teachingClassId)
    const item = myEnrollments.value.find(e => e.teachingClassId === teachingClassId)
    if (item) {
      try { await fetchMine(item.termId) } catch { /* 页面会提示同步失败 */ }
    }
  }

  // 冲突检测计算
  function hasTimeConflict(target: TeachingClassItem): boolean {
    if (!target.schedules || target.schedules.length === 0) return false
    for (const enrolled of activeEnrollments.value) {
      if (enrolled.teachingClassId === target.id) continue
      if (!enrolled.schedules) continue
      for (const s1 of enrolled.schedules) {
        for (const s2 of target.schedules) {
          if (s1.dayOfWeek === s2.dayOfWeek) {
            // 周次重叠判断
            const weekOverlap = !(s1.endWeek < s2.startWeek || s1.startWeek > s2.endWeek)
            // 分钟/节次重叠判断
            const timeOverlap = !(s1.endMinute <= s2.startMinute || s1.startMinute >= s2.endMinute)
            if (weekOverlap && timeOverlap) {
              return true
            }
          }
        }
      }
    }
    return false
  }

  return {
    myEnrollments,
    activeEnrollments,
    totalCredits,
    enrolledClassIds,
    loading,
    fetchMine,
    add,
    remove,
    hasTimeConflict,
  }
})
