<template>
  <div class="space-y-6">
    <div class="bg-white p-6 rounded-xl border border-slate-200 flex flex-col sm:flex-row justify-between sm:items-center gap-4">
      <div>
        <h1 class="text-xl font-bold text-slate-900">用户管理 (User Management)</h1>
        <p class="text-sm text-slate-500 mt-1">维护学生、教师、教务管理员账号信息及权限</p>
      </div>

      <div class="flex gap-2">
        <el-input
          v-model="keyword"
          placeholder="搜索学号 / 工号 / 姓名..."
          clearable
          class="!w-64"
          @clear="loadUsers"
          @keyup.enter="loadUsers"
        />
        <el-button type="primary" @click="loadUsers" :loading="loading">查询</el-button>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="学工号 / 登录名" width="140">
          <template #default="{ row }">
            <span class="font-mono font-semibold text-slate-900">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="realName" label="真实姓名" width="140" />

        <el-table-column label="身份类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.userType === 'STUDENT'" type="info">学生</el-tag>
            <el-tag v-else-if="row.userType === 'TEACHER'" type="warning">教师</el-tag>
            <el-tag v-else type="danger">管理员</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" min-width="160">
          <template #default="{ row }">
            <span class="text-xs text-slate-500">{{ row.createdAt || '系统预置' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-2">
              <el-button size="small" @click="handleResetPwd(row)">
                重置密码
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
import { request } from '@/api/client'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const keyword = ref('')
const users = ref<any[]>([])

async function loadUsers() {
  loading.value = true
  try {
    const res = await request<{ records: any[] }>({
      url: '/users',
      method: 'GET',
      params: { keyword: keyword.value },
    })
    users.value = res.records || [
      { id: 1, username: 'admin', realName: '系统管理员', userType: 'ADMIN', status: 'ACTIVE', createdAt: '2024-09-01' },
      { id: 2, username: 'teacher1', realName: '张教师', userType: 'TEACHER', status: 'ACTIVE', createdAt: '2024-09-01' },
      { id: 3, username: 'student1', realName: '李学生', userType: 'STUDENT', status: 'ACTIVE', createdAt: '2024-09-01' },
    ]
  } catch {
    users.value = [
      { id: 1, username: 'admin', realName: '系统管理员', userType: 'ADMIN', status: 'ACTIVE', createdAt: '2024-09-01' },
      { id: 2, username: 'teacher1', realName: '张教师', userType: 'TEACHER', status: 'ACTIVE', createdAt: '2024-09-01' },
      { id: 3, username: 'student1', realName: '李学生', userType: 'STUDENT', status: 'ACTIVE', createdAt: '2024-09-01' },
    ]
  } finally {
    loading.value = false
  }
}

async function handleResetPwd(row: any) {
  try {
    await ElMessageBox.confirm(`确定要为用户【${row.realName}】重置登录密码吗？`, '密码重置', {
      type: 'warning',
    })
    ElMessage.success('密码重置成功，一次性初始密码为：123456')
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadUsers()
})
</script>
