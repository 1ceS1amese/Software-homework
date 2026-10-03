<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex justify-between items-center">
      <div>
        <h1 class="text-xl font-bold text-slate-900">我的课表 (Timetable)</h1>
        <p class="text-sm text-slate-500 mt-1">周视图展示本学期已成功选入的课程时间安排</p>
      </div>
      <el-button @click="loadSchedule" :loading="loading">刷新课表</el-button>
    </div>

    <!-- Timetable Grid -->
    <div class="bg-white rounded-xl border border-slate-200 p-4 shadow-sm overflow-x-auto">
      <table class="w-full border-collapse border border-slate-200 text-center min-w-[700px]">
        <thead>
          <tr class="bg-slate-50">
            <th class="border border-slate-200 py-3 px-2 w-20 text-xs font-semibold text-slate-600">节次 / 星期</th>
            <th v-for="(day, dIdx) in days" :key="dIdx" class="border border-slate-200 py-3 px-2 text-xs font-semibold text-slate-700">
              {{ day }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="section in sections" :key="section">
            <td class="border border-slate-200 py-4 px-2 bg-slate-50 text-xs font-medium text-slate-500">
              第 {{ section }} 节
            </td>
            <td
              v-for="dayIdx in 7"
              :key="dayIdx"
              class="border border-slate-200 p-1.5 h-20 align-top transition hover:bg-slate-50/50"
            >
              <div
                v-for="item in getCellClasses(dayIdx, section)"
                :key="item.id"
                class="bg-indigo-50 border border-indigo-200 text-indigo-900 p-2 rounded text-left text-xs mb-1 shadow-xs"
              >
                <div class="font-bold truncate">{{ item.courseName }}</div>
                <div class="text-[11px] text-indigo-700 truncate">📍 {{ item.location || '教室待定' }}</div>
                <div class="text-[10px] text-indigo-500">{{ item.teacherName }}</div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyScheduleApi } from '@/api/enroll'
import type { TeachingClassItem } from '@/api/types'

const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const sections = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
const loading = ref(false)
const scheduleList = ref<TeachingClassItem[]>([])

async function loadSchedule() {
  loading.value = true
  try {
    const res = await getMyScheduleApi()
    scheduleList.value = res || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function getCellClasses(dayOfWeek: number, section: number) {
  return scheduleList.value.filter((cls: TeachingClassItem) => {
    if (!cls.schedules || cls.schedules.length === 0) return false
    return cls.schedules.some((s: any) => s.dayOfWeek === dayOfWeek && section >= s.startSection && section <= s.endSection)
  })
}

onMounted(() => {
  loadSchedule()
})
</script>
