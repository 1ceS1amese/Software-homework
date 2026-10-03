<template>
  <div class="min-h-screen flex bg-slate-50 font-sans text-slate-800">
    <!-- Admin Sidebar -->
    <aside class="w-64 bg-slate-900 text-slate-300 flex flex-col shrink-0">
      <div class="h-16 flex items-center px-6 bg-slate-950 font-bold text-white tracking-wide border-b border-slate-800">
        <span class="text-amber-400 mr-2 text-xl">⚙️</span> 教务管理中台
      </div>

      <nav class="flex-1 px-4 py-4 space-y-1 overflow-y-auto text-sm">
        <div class="text-[11px] font-semibold uppercase tracking-wider text-slate-500 px-3 py-1.5">核心监控</div>
        <router-link
          to="/admin/dashboard"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>📊</span> 概览看板
        </router-link>
        <router-link
          to="/admin/stats"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>📈</span> 统计报表
        </router-link>

        <div class="text-[11px] font-semibold uppercase tracking-wider text-slate-500 px-3 pt-3 py-1.5">教务资源</div>
        <router-link
          to="/admin/users"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>👥</span> 用户管理
        </router-link>
        <router-link
          to="/admin/depts"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>🏢</span> 院系与专业
        </router-link>
        <router-link
          to="/admin/terms"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>🗓️</span> 学期管理
        </router-link>
        <router-link
          to="/admin/courses"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>📖</span> 课程管理
        </router-link>
        <router-link
          to="/admin/teaching-classes"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>🏫</span> 教学班管理
        </router-link>

        <div class="text-[11px] font-semibold uppercase tracking-wider text-slate-500 px-3 pt-3 py-1.5">系统与安全</div>
        <router-link
          to="/admin/audit-logs"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>🛡️</span> 审计日志
        </router-link>
        <router-link
          to="/admin/login-logs"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>🔐</span> 登录日志
        </router-link>
        <router-link
          to="/admin/configs"
          class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
          active-class="bg-indigo-600 text-white hover:bg-indigo-600"
        >
          <span>⚙️</span> 系统参数
        </router-link>

        <div class="pt-3 border-t border-slate-800">
          <router-link
            to="/profile"
            class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>👤</span> 个人资料
          </router-link>
          <router-link
            to="/profile/password"
            class="flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>🔑</span> 修改密码
          </router-link>
        </div>
      </nav>

      <!-- User footer -->
      <div class="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-3 overflow-hidden">
          <div class="w-8 h-8 rounded-full bg-amber-500/20 text-amber-400 font-bold flex items-center justify-center shrink-0">
            A
          </div>
          <div class="truncate">
            <div class="text-xs font-medium text-white truncate">{{ authStore.user?.realName || authStore.user?.username }}</div>
            <div class="text-[11px] text-amber-400">系统超级管理员</div>
          </div>
        </div>
        <button
          @click="handleLogout"
          class="p-1.5 text-slate-400 hover:text-rose-400 rounded-lg hover:bg-slate-800 transition cursor-pointer"
          title="退出登录"
        >
          🚪
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-w-0">
      <header class="h-16 bg-white border-b border-slate-200 px-8 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="text-xs font-semibold text-slate-500 uppercase">管理学期上下文：</span>
          <el-select
            v-model="termStore.currentTermId"
            placeholder="请选择学期"
            size="small"
            class="!w-56"
            @change="(val: number) => termStore.switchTerm(val)"
          >
            <el-option
              v-for="t in termStore.terms"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </div>

        <div class="flex items-center gap-4">
          <span class="text-xs text-slate-500">
            身份认证模式：<strong class="text-indigo-600">RBAC 单点鉴权</strong>
          </span>
        </div>
      </header>

      <main class="flex-1 p-8 overflow-y-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTermStore } from '@/stores/term'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'

const authStore = useAuthStore()
const termStore = useTermStore()
const router = useRouter()

onMounted(async () => {
  await termStore.fetchTerms()
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出管理员账号吗？', '提示', {
      type: 'warning',
    })
    await authStore.logout()
    ElMessage.success('已安全退出')
    router.push('/login')
  } catch {
    // cancelled
  }
}
</script>
