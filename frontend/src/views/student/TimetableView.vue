<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
      <div>
        <h1 class="text-xl font-bold text-slate-900">我的课表 (Timetable)</h1>
        <p class="text-sm text-slate-500 mt-1">周视图展示本学期已成功选入的课程时间排布</p>
      </div>

      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2 text-xs bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200">
          <span>已选门数：<strong class="text-slate-800">{{ enrolledClasses.length }}</strong></span>
          <span class="text-slate-300">|</span>
          <span>已选学分：<strong class="text-indigo-600">{{ totalCredits }}</strong></span>
        </div>
        <UButton color="neutral" variant="outline" :loading="loading" @click="loadData">刷新课表</UButton>
      </div>
    </div>

    <!-- Empty State -->
    <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="课表加载失败" :description="error" />
    <div v-if="enrolledClasses.length === 0 && !loading && !error" class="bg-white rounded-xl border border-slate-200 p-8">
      <EmptyState
        icon="📅"
        text="你当前学期还没有选择任何课程"
        sub-text="前往选课中心查看开放教学班并完成在线选课"
      >
        <template #action>
          <router-link to="/student/courses">
            <UButton>去选课</UButton>
          </router-link>
        </template>
      </EmptyState>
    </div>

    <!-- Timetable Grid -->
    <div v-else-if="enrolledClasses.length" class="bg-white rounded-xl border border-slate-200 p-4 shadow-sm overflow-x-auto">
      <table class="w-full border-collapse border border-slate-200 text-center min-w-[750px]">
        <thead>
          <tr class="bg-slate-50">
            <th class="border border-slate-200 py-3 px-2 w-20 text-xs font-semibold text-slate-600">节次 / 星期</th>
            <th v-for="(day, dIdx) in days" :key="dIdx" class="border border-slate-200 py-3 px-2 text-xs font-semibold text-slate-700">
              {{ day }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="section in 12" :key="section">
            <td class="border border-slate-200 py-3 px-2 bg-slate-50 text-xs font-medium text-slate-500">
              第 {{ section }} 节
            </td>
            <td
              v-for="dayIdx in 7"
              :key="dayIdx"
              class="border border-slate-200 p-1.5 h-16 align-top transition hover:bg-slate-50/50"
            >
              <div
                v-for="item in getCellClasses(dayIdx, section)"
                :key="item.id"
                class="bg-indigo-50 border border-indigo-200 text-indigo-900 p-1.5 rounded text-left text-xs mb-1 shadow-2xs cursor-pointer hover:bg-indigo-100 transition"
                @click="openDetail(item)"
              >
                <div class="font-bold truncate text-[11px]">{{ item.courseName }}</div>
                <div class="text-[10px] text-indigo-700 truncate">📍 {{ item.location || '待定' }}</div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Drawer for Class Detail -->
    <UModal v-model:open="drawerVisible" title="教学班详情">
      <template #body>
      <template v-if="selectedClass">
        <div class="space-y-4">
          <div class="border-b border-slate-100 pb-3">
            <h3 class="text-lg font-bold text-slate-900">{{ selectedClass.courseName }}</h3>
            <p class="text-xs text-slate-500">班号: {{ selectedClass.className }} | 代码: {{ selectedClass.courseCode }}</p>
          </div>

          <dl class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-3 text-sm">
            <dt class="text-(--ui-text-muted)">任课教师</dt><dd>{{ selectedClass.teacherName }}</dd>
            <dt class="text-(--ui-text-muted)">课程学分</dt><dd>{{ selectedClass.credit }} 学分</dd>
            <dt class="text-(--ui-text-muted)">上课周次</dt><dd>第 {{ selectedClass.startWeek }}–{{ selectedClass.endWeek }} 周</dd>
            <dt class="text-(--ui-text-muted)">上课教室</dt><dd>{{ selectedClass.location || '待定教室' }}</dd>
          </dl>

          <div class="pt-6">
            <UButton color="error" block :disabled="!termStore.isWithdrawAllowedNow" @click="confirmOpen = true">退选本门课程</UButton>
          </div>
        </div>
      </template>
      </template>
    </UModal>
    <UModal v-model:open="confirmOpen" title="确认退课" :description="selectedClass ? `确定退出“${selectedClass.courseName}”吗？` : ''">
      <template #footer><UButton color="neutral" variant="outline" @click="confirmOpen = false">取消</UButton><UButton color="error" :loading="withdrawLoading" @click="handleWithdraw">确认退课</UButton></template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getMySchedule, withdrawClass } from '@/api/enrollments'
import { useToast } from '@nuxt/ui/composables'
import { useTermStore } from '@/stores/term'
import type { TeachingClassItem } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'

const termStore = useTermStore()
const toast = useToast()
const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const loading = ref(false)
const withdrawLoading = ref(false)
const drawerVisible = ref(false)
const confirmOpen = ref(false)
const error = ref('')
const enrolledClasses = ref<TeachingClassItem[]>([])
const selectedClass = ref<TeachingClassItem | null>(null)

const totalCredits = computed(() => {
  return enrolledClasses.value.reduce((acc, curr) => acc + (curr.credit || 0), 0)
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await getMySchedule(termStore.currentTermId || undefined)
    enrolledClasses.value = res || []
  } catch {
    enrolledClasses.value = []
    error.value = '请检查网络或后端服务，然后重试。'
  } finally {
    loading.value = false
  }
}

function getCellClasses(dayOfWeek: number, section: number) {
  return enrolledClasses.value.filter(cls => {
    if (!cls.schedules || cls.schedules.length === 0) return false
    return cls.schedules.some(s => s.dayOfWeek === dayOfWeek && section >= s.startSection && section <= s.endSection)
  })
}

function openDetail(item: TeachingClassItem) {
  selectedClass.value = item
  drawerVisible.value = true
}

async function handleWithdraw() {
  if (!selectedClass.value) return
  try {
    withdrawLoading.value = true
    await withdrawClass(selectedClass.value.id)
    toast.add({ title: '已成功退课', color: 'success' })
    confirmOpen.value = false
    drawerVisible.value = false
    await loadData()
  } catch { /* 请求层已提示 */ }
  finally {
    withdrawLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
