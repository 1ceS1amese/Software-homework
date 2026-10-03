<template>
  <div class="min-h-screen bg-(--ui-bg-muted) text-(--ui-text) lg:flex">
    <div v-if="menuOpen" class="fixed inset-0 z-40 bg-slate-950/50 lg:hidden" @click="menuOpen = false" />
    <aside
      :class="menuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'"
      class="fixed inset-y-0 left-0 z-50 flex w-66 shrink-0 flex-col border-r border-slate-800 bg-slate-950 text-slate-200 transition-transform lg:sticky lg:top-0 lg:h-screen"
    >
      <div class="flex h-20 items-center gap-3 border-b border-slate-800 px-6">
        <div class="grid size-10 place-items-center rounded-xl bg-teal-700 text-white">
          <UIcon name="i-lucide-graduation-cap" class="size-6" />
        </div>
        <div class="min-w-0">
          <div class="text-base font-semibold tracking-tight text-white">CSMS 教务平台</div>
          <div class="text-xs text-slate-400">课程 · 选课 · 成绩</div>
        </div>
        <UButton class="ml-auto lg:hidden" icon="i-lucide-x" color="neutral" variant="ghost" aria-label="关闭菜单" @click="menuOpen = false" />
      </div>

      <nav class="min-h-0 flex-1 overflow-y-auto px-3 py-5" aria-label="主导航">
        <div class="mb-3 px-3 text-xs font-medium tracking-wide text-slate-400">{{ sectionTitle }}</div>
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="mb-1 flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm font-medium text-slate-300 transition-colors hover:bg-slate-800 hover:text-white"
          active-class="!bg-teal-700 !text-white"
          @click="menuOpen = false"
        >
          <UIcon :name="item.icon" class="size-5 shrink-0" />
          <span>{{ item.title }}</span>
        </RouterLink>
      </nav>

      <div class="border-t border-slate-800 p-4">
        <div class="flex items-center gap-3 rounded-lg bg-slate-900 p-3">
          <UAvatar :alt="displayName" :text="displayName.slice(0, 1)" size="sm" />
          <div class="min-w-0 flex-1">
            <div class="truncate text-sm font-medium text-white">{{ displayName }}</div>
            <div class="text-xs text-slate-400">{{ roleLabel }}</div>
          </div>
          <UButton icon="i-lucide-log-out" color="neutral" variant="ghost" aria-label="退出登录" @click="handleLogout" />
        </div>
      </div>
    </aside>

    <div class="min-w-0 flex-1">
      <header class="sticky top-0 z-30 flex min-h-16 flex-wrap items-center gap-3 border-b border-(--ui-border) bg-(--ui-bg)/95 px-4 py-3 backdrop-blur sm:px-6 lg:px-8">
        <UButton class="lg:hidden" icon="i-lucide-menu" color="neutral" variant="ghost" aria-label="打开菜单" @click="menuOpen = true" />
        <div class="min-w-0 flex-1">
          <div class="truncate text-sm font-semibold text-(--ui-text-highlighted)">{{ currentTitle }}</div>
          <div class="hidden text-xs text-(--ui-text-muted) sm:block">教学管理 / {{ sectionTitle }}</div>
        </div>
        <label for="global-term" class="hidden text-sm text-(--ui-text-muted) sm:block">学期</label>
        <USelect
          id="global-term"
          v-model="selectedTermId"
          :items="termItems"
          :loading="termStore.loading"
          placeholder="暂无学期"
          class="w-36 max-w-full sm:w-58"
          aria-label="选择学期"
        />
        <UBadge color="primary" variant="subtle" class="hidden sm:inline-flex">{{ roleLabel }}</UBadge>
      </header>
      <div v-if="uiStore.isLoading" class="h-0.5 bg-teal-100" role="progressbar" aria-label="正在加载"><div class="h-full w-1/3 animate-pulse bg-teal-700" /></div>
      <main class="mx-auto w-full min-w-0 max-w-360 p-4 sm:p-6 lg:p-8">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTermStore } from '@/stores/term'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const termStore = useTermStore()
const uiStore = useUiStore()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)

const displayName = computed(() => authStore.user?.realName || authStore.user?.username || '用户')
const roleLabel = computed(() => ( { STUDENT: '学生中心', TEACHER: '教师工作台', ADMIN: '教务管理' } as Record<string, string>)[authStore.userType] || '个人中心')
const sectionTitle = computed(() => roleLabel.value)
const currentTitle = computed(() => String(route.meta.title || '个人中心'))
const navItems = computed(() => router.getRoutes()
  .filter(r => r.meta.nav && (r.meta.roles as string[] | undefined)?.includes(authStore.userType))
  .map(r => ({ path: r.path, title: String(r.meta.title), icon: String(r.meta.icon || 'i-lucide-circle') })))
const termItems = computed(() => termStore.terms.map(t => ({ label: t.name, value: t.id })))
const selectedTermId = computed<number | undefined>({
  get: () => termStore.currentTermId ?? undefined,
  set: value => { if (value != null) termStore.switchTerm(value) },
})

onMounted(() => { void termStore.fetchTerms() })

async function handleLogout() {
  await authStore.logout()
  await router.replace('/login')
}
</script>
