<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex justify-between items-center">
      <div>
        <h1 class="text-xl font-bold text-slate-900">我的教学班 (Teaching Classes)</h1>
        <p class="text-sm text-slate-500 mt-1">查看本人本学期所授课程班级、选课人数以及执行状态流转</p>
      </div>
      <el-button type="primary" @click="loadClasses" :loading="loading">刷新列表</el-button>
    </div>

    <div class="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
      <el-table :data="classes" v-loading="loading" style="width: 100%">
        <el-table-column prop="className" label="教学班名称" min-width="150">
          <template #default="{ row }">
            <div class="font-semibold text-slate-900">{{ row.className }}</div>
            <div class="text-xs text-slate-400">代码: {{ row.courseCode }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="courseName" label="课程名称" min-width="160" />

        <el-table-column prop="credit" label="学分" width="80" />

        <el-table-column label="选课情况" width="160">
          <template #default="{ row }">
            <div class="text-xs text-slate-600 mb-1">
              已选 {{ row.enrolledCount }} / 容量 {{ row.capacity }}
            </div>
            <el-progress
              :percentage="Math.min(100, Math.round((row.enrolledCount / row.capacity) * 100))"
              :stroke-width="6"
              :show-text="false"
            />
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PUBLISHED'" type="success">已发布选课</el-tag>
            <el-tag v-else-if="row.status === 'DRAFT'" type="info">草稿</el-tag>
            <el-tag v-else-if="row.status === 'CLOSED'" type="warning">已结课</el-tag>
            <el-tag v-else type="danger">已取消</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-2">
              <el-button
                size="small"
                type="primary"
                @click="goToGrades(row.id)"
              >
                录入成绩
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeachingClassesApi } from '@/api/classroom'
import { useAuthStore } from '@/stores/auth'
import type { TeachingClassItem } from '@/api/types'

const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const classes = ref<TeachingClassItem[]>([])

async function loadClasses() {
  loading.value = true
  try {
    const res = await getTeachingClassesApi({
      teacherId: authStore.user?.id,
    })
    classes.value = res.records || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function goToGrades(classId: number) {
  router.push({ path: '/teacher/grades/entry', query: { classId } })
}

onMounted(() => {
  loadClasses()
})
</script>
