<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
      <div>
        <h1 class="text-xl font-bold text-slate-900">成绩录入 (Grade Entry)</h1>
        <p class="text-sm text-slate-500 mt-1">
          当前教学班：<span class="font-bold text-indigo-600">ID: {{ classId || '未指定' }}</span>
          <span v-if="isPublished" class="ml-2 text-rose-500 font-semibold">🔒 成绩已发布，禁止修改（需管理员解锁）</span>
        </p>
      </div>

      <div class="flex gap-2">
        <el-button @click="loadGrades" :loading="loading">刷新</el-button>
        <el-button
          type="primary"
          :disabled="isPublished || grades.length === 0"
          :loading="saving"
          @click="handleSave"
        >
          暂存成绩
        </el-button>
        <el-button
          type="success"
          :disabled="isPublished || grades.length === 0"
          :loading="publishing"
          @click="handlePublish"
        >
          正式发布成绩
        </el-button>
      </div>
    </div>

    <!-- Alert for BR-11 & BR-12 -->
    <el-alert
      title="录入规则提示：权重为 平时(30%) + 期中(30%) + 期末(40%)。表格支持实时动态预览总评，正式发布后将对学生可见并锁定，请仔细核对。"
      type="info"
      show-icon
      :closable="false"
    />

    <!-- Grades Table -->
    <div class="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
      <el-table :data="grades" v-loading="loading" style="width: 100%">
        <el-table-column prop="studentId" label="学号 / 学生ID" width="140" />

        <el-table-column label="平时成绩 (30%)" min-width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.regularScore"
              :min="0"
              :max="100"
              :precision="1"
              :disabled="isPublished"
              size="small"
              @change="computeTotal(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="期中成绩 (30%)" min-width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.midtermScore"
              :min="0"
              :max="100"
              :precision="1"
              :disabled="isPublished"
              size="small"
              @change="computeTotal(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="期末成绩 (40%)" min-width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.finalScore"
              :min="0"
              :max="100"
              :precision="1"
              :disabled="isPublished"
              size="small"
              @change="computeTotal(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="总评 (预览)" width="120">
          <template #default="{ row }">
            <span class="font-bold text-base" :class="(row.totalScore ?? 0) >= 60 ? 'text-indigo-600' : 'text-rose-600'">
              {{ row.totalScore ?? '-' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PUBLISHED'" type="success">已发布</el-tag>
            <el-tag v-else type="info">草稿</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getClassGradesApi, saveGradesApi, publishGradesApi } from '@/api/grade'
import type { GradeItem } from '@/api/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const classId = ref(Number(route.query.classId) || 1)
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const grades = ref<GradeItem[]>([])

const isPublished = computed(() => {
  return grades.value.length > 0 && grades.value.every((g: GradeItem) => g.status === 'PUBLISHED')
})

function computeTotal(row: GradeItem) {
  const reg = row.regularScore ?? 0
  const mid = row.midtermScore ?? 0
  const fin = row.finalScore ?? 0
  row.totalScore = Number((reg * 0.3 + mid * 0.3 + fin * 0.4).toFixed(1))
}

async function loadGrades() {
  if (!classId.value) return
  loading.value = true
  try {
    const res = await getClassGradesApi(classId.value)
    grades.value = res || []
    grades.value.forEach(computeTotal)
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveGradesApi(classId.value, grades.value)
    ElMessage.success('成绩草稿暂存成功')
    await loadGrades()
  } catch {
    // handled
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  try {
    await ElMessageBox.confirm('成绩发布后将直接向学生公示，并锁定修改权限。确认发布？', '发布确认 (BR-12)', {
      confirmButtonText: '确定发布',
      cancelButtonText: '取消',
      type: 'warning',
    })
    publishing.value = true
    await publishGradesApi(classId.value)
    ElMessage.success('成绩已正式发布！')
    await loadGrades()
  } catch {
    // cancelled
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  loadGrades()
})
</script>
