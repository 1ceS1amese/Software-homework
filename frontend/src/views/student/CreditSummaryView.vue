<template>
  <PageContainer title="学分与学业概览" description="依据已选课程与正式发布的成绩计算，数据按当前学期筛选。">
    <template #actions><UButton color="neutral" variant="outline" icon="i-lucide-refresh-cw" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="space-y-5">
      <UAlert v-if="error" color="error" variant="soft" icon="i-lucide-circle-alert" title="学分统计加载失败" :description="error" />
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="item in cards" :key="item.label" class="rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5">
          <p class="text-sm text-(--ui-text-muted)">{{ item.label }}</p>
          <p class="mt-3 text-3xl font-semibold tabular-nums text-(--ui-text-highlighted)">{{ item.value }}</p>
          <p class="mt-2 text-xs text-(--ui-text-muted)">{{ item.hint }}</p>
        </div>
      </div>
      <UAlert color="info" variant="soft" icon="i-lucide-info" title="毕业学分进度暂未提供" description="后端尚未提供培养方案中的必修、选修及实践学分要求，因此不显示虚构的毕业进度。" />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getStudentSummary } from '@/api/stats'
import { useAuthStore } from '@/stores/auth'
import { useTermStore } from '@/stores/term'
import type { StudentCreditSummary } from '@/api/types'
import PageContainer from '@/components/PageContainer.vue'

const authStore = useAuthStore()
const termStore = useTermStore()
const summary = ref<StudentCreditSummary | null>(null)
const loading = ref(false)
const error = ref('')
const cards = computed(() => [
  { label: '本学期已选学分', value: summary.value?.totalEnrolledCredits ?? '—', hint: '以当前选课记录为准' },
  { label: '已获学分', value: summary.value?.earnedCredits ?? '—', hint: '通过且已发布成绩的课程' },
  { label: '平均学分绩点', value: summary.value?.gpa ?? '—', hint: '由后端按学分加权计算' },
  { label: '考核通过率', value: summary.value ? (summary.value.courseCount ? `${Math.round(summary.value.passCount / summary.value.courseCount * 100)}%` : '—') : '—', hint: summary.value ? `未通过 ${summary.value.failCount} 门` : '暂无数据' },
])
async function loadData() {
  loading.value = true
  error.value = ''
  try {
    if (!authStore.user?.id) throw new Error('用户信息不可用')
    summary.value = await getStudentSummary(authStore.user.id, termStore.currentTermId ?? undefined)
  } catch {
    summary.value = null
    error.value = '请检查网络或后端服务，然后重试。'
  } finally { loading.value = false }
}
onMounted(() => { void loadData() })
</script>
