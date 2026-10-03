<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex justify-between items-center">
      <div>
        <h1 class="text-xl font-bold text-slate-900">成绩查询 (My Grades)</h1>
        <p class="text-sm text-slate-500 mt-1">
          平均学分绩点 (GPA)：<span class="font-bold text-indigo-600 text-base">{{ averageGPA }}</span>
        </p>
      </div>
      <el-button @click="loadGrades" :loading="loading">刷新成绩</el-button>
    </div>

    <div class="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
      <el-table :data="grades" v-loading="loading" style="width: 100%">
        <el-table-column prop="courseName" label="课程名称" min-width="160">
          <template #default="{ row }">
            <span class="font-semibold text-slate-800">{{ row.courseName || '课程' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="regularScore" label="平时成绩" width="100">
          <template #default="{ row }">{{ row.regularScore ?? '-' }}</template>
        </el-table-column>

        <el-table-column prop="midtermScore" label="期中成绩" width="100">
          <template #default="{ row }">{{ row.midtermScore ?? '-' }}</template>
        </el-table-column>

        <el-table-column prop="finalScore" label="期末成绩" width="100">
          <template #default="{ row }">{{ row.finalScore ?? '-' }}</template>
        </el-table-column>

        <el-table-column prop="totalScore" label="综合总评" width="120">
          <template #default="{ row }">
            <span class="font-bold" :class="(row.totalScore ?? 0) >= 60 ? 'text-slate-900' : 'text-rose-600'">
              {{ row.totalScore ?? '尚未录入' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="gradePoint" label="绩点 (GPA)" width="120">
          <template #default="{ row }">
            <span class="font-mono text-indigo-600 font-semibold">{{ row.gradePoint ?? '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="考核结果" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.isPass === 1" type="success" effect="light">通过</el-tag>
            <el-tag v-else-if="row.isPass === 0" type="danger" effect="light">未通过</el-tag>
            <el-tag v-else type="info" effect="light">待评定</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getMyGradesApi } from '@/api/grade'
import type { GradeItem } from '@/api/types'

const loading = ref(false)
const grades = ref<GradeItem[]>([])

const averageGPA = computed(() => {
  const published = grades.value.filter((g: GradeItem) => g.gradePoint !== undefined && g.gradePoint !== null)
  if (published.length === 0) return '0.00'
  const sum = published.reduce((acc: number, curr: GradeItem) => acc + (curr.gradePoint || 0), 0)
  return (sum / published.length).toFixed(2)
})

async function loadGrades() {
  loading.value = true
  try {
    const res = await getMyGradesApi()
    grades.value = res || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadGrades()
})
</script>
