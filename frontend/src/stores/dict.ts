import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDepts, getMajors } from '@/api/base'
import type { DeptItem, MajorItem } from '@/api/types'

export const useDictStore = defineStore('dict', () => {
  const depts = ref<DeptItem[]>([])
  const majors = ref<MajorItem[]>([])
  const courseTypes = ref([
    { label: '必修课', value: 'REQUIRED' },
    { label: '选修课', value: 'ELECTIVE' },
    { label: '限选课', value: 'RESTRICTED' },
  ])

  async function fetchDepts() {
    try {
      const list = await getDepts()
      depts.value = list || []
    } catch {
      // fallback
    }
  }

  async function fetchMajors(deptId?: number) {
    try {
      const list = await getMajors({ deptId })
      majors.value = list || []
    } catch {
      // fallback
    }
  }

  return {
    depts,
    majors,
    courseTypes,
    fetchDepts,
    fetchMajors,
  }
})
