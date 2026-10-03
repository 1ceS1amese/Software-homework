<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex justify-between items-center">
      <div>
        <h1 class="text-xl font-bold text-slate-900">课程管理 (Courses)</h1>
        <p class="text-sm text-slate-500 mt-1">维护学校标准课程库、学分学时以及先修关系（成环校验拦截）</p>
      </div>
      <el-button type="primary" @click="handleCreate">新建课程</el-button>
    </div>

    <div class="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
      <el-table :data="courses" v-loading="loading" style="width: 100%">
        <el-table-column prop="courseCode" label="课程代码" width="140">
          <template #default="{ row }">
            <span class="font-mono font-semibold">{{ row.courseCode }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="课程名称" min-width="160" />

        <el-table-column prop="credit" label="学分" width="100" />

        <el-table-column prop="creditHours" label="总学时" width="100" />

        <el-table-column label="课程性质" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.courseType === 'REQUIRED'" type="danger">必修</el-tag>
            <el-tag v-else-if="row.courseType === 'ELECTIVE'" type="success">选修</el-tag>
            <el-tag v-else type="warning">限选</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="先修课程要求" min-width="160">
          <template #default="{ row }">
            <span class="text-xs text-slate-500">{{ row.prereqs || '无' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const courses = ref([
  { id: 1, courseCode: 'CS101', name: '高等数学 (上)', credit: 5.0, creditHours: 80, courseType: 'REQUIRED', prereqs: '无' },
  { id: 2, courseCode: 'CS102', name: 'C语言程序设计', credit: 4.0, creditHours: 64, courseType: 'REQUIRED', prereqs: '无' },
  { id: 3, courseCode: 'CS201', name: '数据结构与算法', credit: 4.0, creditHours: 64, courseType: 'REQUIRED', prereqs: 'C语言程序设计 (CS102)' },
  { id: 4, courseCode: 'CS301', name: '计算机网络', credit: 3.5, creditHours: 56, courseType: 'ELECTIVE', prereqs: '数据结构与算法 (CS201)' },
])

function handleCreate() {
  ElMessage.info('新建课程弹窗（支持先修课程多选与防成环算法检测）')
}
</script>
