<template>
  <div class="min-h-screen flex bg-slate-50 font-sans text-slate-800">
    <!-- Sidebar -->
    <aside class="w-64 bg-slate-900 text-slate-300 flex flex-col shrink-0">
      <div class="h-16 flex items-center px-6 bg-slate-950 font-bold text-white tracking-wide border-b border-slate-800">
        <span class="text-indigo-400 mr-2 text-xl">🎓</span> CSMS 选课管理系统
      </div>

      <nav class="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
        <!-- Student Menu -->
        <template v-if="authStore.isStudent">
          <div class="text-xs font-semibold uppercase tracking-wider text-slate-500 px-3 py-2">学生中心</div>
          <router-link
            to="/student/courses"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>🎯</span> 课程选择
          </router-link>
          <router-link
            to="/student/timetable"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>📅</span> 我的课表
          </router-link>
          <router-link
            to="/student/grades"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>📊</span> 我的成绩
          </router-link>
          <router-link
            to="/student/summary"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>📈</span> 我的学分统计
          </router-link>
        </template>

        <!-- Teacher Menu -->
        <template v-if="authStore.isTeacher">
          <div class="text-xs font-semibold uppercase tracking-wider text-slate-500 px-3 py-2">教师教学</div>
          <router-link
            to="/teacher/classes"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>🏫</span> 我的教学班
          </router-link>
        </template>

        <!-- Common Section -->
        <div class="pt-4 mt-4 border-t border-slate-800">
          <div class="text-xs font-semibold uppercase tracking-wider text-slate-500 px-3 py-2">个人中心</div>
          <router-link
            to="/profile"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>👤</span> 个人资料
          </router-link>
          <router-link
            to="/profile/password"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition hover:bg-slate-800 hover:text-white"
            active-class="bg-indigo-600 text-white hover:bg-indigo-600"
          >
            <span>🔑</span> 修改密码
          </router-link>
        </div>
      </nav>

      <!-- User footer -->
      <div class="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-3 overflow-hidden">
          <div class="w-8 h-8 rounded-full bg-indigo-500/20 text-indigo-400 font-bold flex items-center justify-center shrink-0">
            {{ (authStore.user?.realName || authStore.user?.username || 'U')[0] }}
          </div>
          <div class="truncate">
            <div class="text-xs font-medium text-white truncate">{{ authStore.user?.realName || authStore.user?.username }}</div>
            <div class="text-[11px] text-slate-400 uppercase">{{ authStore.userType }}</div>
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
      <!-- Topbar with Term Selector -->
      <header class="h-16 bg-white border-b border-slate-200 px-8 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <span class="text-xs font-semibold text-slate-500 uppercase">当前学期：</span>
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
          <span v-if="termStore.isEnrollingNow" class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
            ⚡ 选课进行中
          </span>
          <span v-else class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-600">
            🔒 非选课开放期
          </span>
        </div>

        <div class="flex items-center gap-4">
          <span class="text-xs text-slate-500">
            欢迎您，<strong class="text-slate-800">{{ authStore.user?.realName }}</strong>
          </span>
        </div>
      </header>

      <!-- Page Content -->
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
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // cancelled
  }
}
</script>
