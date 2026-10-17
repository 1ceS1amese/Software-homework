<template>
  <PageContainer title="成绩管理与解锁" description="查看成绩发布状态。解锁后学生端撤下结果，教师可更正后重新发布；所有解锁必须填写原因。">
    <template #actions><UButton color="neutral" variant="outline" :loading="loading" @click="loadData">刷新</UButton></template>
    <div class="flex flex-wrap gap-3 pb-4"><UInput v-model="keyword" placeholder="搜索教学班或课程" aria-label="搜索成绩教学班" @keyup.enter="search" /><select v-model="termId" aria-label="成绩学期" class="rounded-md border border-(--ui-border) bg-(--ui-bg) p-2" @change="search"><option v-for="term in terms" :key="term.id" :value="String(term.id)">{{ term.name }}</option></select><UButton @click="search">查询</UButton></div>
    <UAlert v-if="error" color="error" title="成绩状态加载失败" :description="error" />
    <div class="overflow-x-auto rounded-xl border border-(--ui-border)"><UTable :data="classes" :columns="columns" :loading="loading" class="min-w-180">
      <template #status-cell="{ row }"><UBadge :color="row.original.gradeStatus === 'PUBLISHED' ? 'success' : 'neutral'">{{ row.original.gradeStatus === 'PUBLISHED' ? '已发布' : row.original.gradeStatus === 'DRAFT' ? '草稿' : '未录入' }}</UBadge></template>
      <template #actions-cell="{ row }"><div class="flex gap-2"><UButton size="sm" variant="outline" @click="view(row.original)">查看成绩</UButton><UButton size="sm" color="warning" :disabled="!row.original.publishedGradeCount" @click="pending = row.original; reason = ''">解锁</UButton></div></template>
    </UTable><div class="flex justify-between p-4"><span>共 {{ total }} 个 · 第 {{ page }} 页</span><div class="flex gap-2"><UButton :disabled="page === 1" @click="page--; loadData()">上一页</UButton><UButton :disabled="page * 20 >= total" @click="page++; loadData()">下一页</UButton></div></div></div>
  </PageContainer>
  <UModal v-model:open="viewOpen" title="教学班成绩详情" :description="selected?.className"><template #body><div class="overflow-x-auto"><UTable :data="grades" :columns="gradeColumns" class="min-w-150" /></div></template></UModal>
  <UModal v-model:open="unlockOpen" title="管理员解锁成绩" :description="pending?.className"><template #body><form id="unlock-grades" class="grid gap-3" @submit.prevent="unlock"><label class="grid gap-1">解锁原因<textarea v-model="reason" aria-label="解锁原因" minlength="5" maxlength="300" required class="rounded-md border border-(--ui-border) bg-(--ui-bg) p-2" /></label><p class="text-sm text-(--ui-text-muted)">至少5字。解锁保留已有分数和操作记录，学生结果将在重新发布前撤下。</p></form></template><template #footer><UButton color="neutral" variant="outline" @click="pending = null">取消</UButton><UButton type="submit" form="unlock-grades" color="warning" :disabled="reason.trim().length < 5 || saving" :loading="saving">确认解锁</UButton></template></UModal>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { TableColumn } from '@nuxt/ui'
import { useToast } from '@nuxt/ui/composables'
import { getTerms } from '@/api/base'
import { getTeachingClasses } from '@/api/teachingClasses'
import { getClassGrades, unlockGrades } from '@/api/grades'
import type { TermItem, TeachingClassItem, GradeItem } from '@/api/types'
import { useTermStore } from '@/stores/term'
import PageContainer from '@/components/PageContainer.vue'
const terms = ref<TermItem[]>([]), termId = ref(''), keyword = ref(''), classes = ref<TeachingClassItem[]>([]), total = ref(0), page = ref(1)
const loading = ref(false), saving = ref(false), error = ref(''), reason = ref(''), selected = ref<TeachingClassItem | null>(null), pending = ref<TeachingClassItem | null>(null), grades = ref<GradeItem[]>([])
const viewOpen = computed({ get: () => !!selected.value, set: v => { if (!v) selected.value = null } })
const unlockOpen = computed({ get: () => !!pending.value, set: v => { if (!v) pending.value = null } })
const toast = useToast()
const columns: TableColumn<TeachingClassItem>[] = [{ accessorKey: 'className', header: '教学班' }, { accessorKey: 'courseName', header: '课程' }, { accessorKey: 'teacherName', header: '教师' }, { id: 'status', header: '成绩状态' }, { accessorKey: 'publishedGradeCount', header: '已发布人数' }, { id: 'actions', header: '操作' }]
const gradeColumns: TableColumn<GradeItem>[] = [{ accessorKey: 'studentNumber', header: '学号' }, { accessorKey: 'studentName', header: '姓名' }, { accessorKey: 'regularScore', header: '平时' }, { accessorKey: 'midtermScore', header: '期中' }, { accessorKey: 'finalScore', header: '期末' }, { accessorKey: 'totalScore', header: '总评' }, { accessorKey: 'status', header: '状态' }]
function search() { page.value = 1; void loadData() }
async function loadData() { if (!termId.value) return; loading.value = true; error.value = ''; try { const result = await getTeachingClasses({ termId: Number(termId.value), keyword: keyword.value || undefined, page: page.value, size: 20 }); classes.value = result.records; total.value = result.total } catch { error.value = '请检查服务后重试。' } finally { loading.value = false } }
async function view(row: TeachingClassItem) { try { const data = await getClassGrades(row.id); grades.value = data; selected.value = row } catch { /* 请求层提示 */ } }
async function unlock() { if (!pending.value || saving.value || reason.value.trim().length < 5) return; saving.value = true; try { await unlockGrades(pending.value.id, reason.value.trim()); pending.value = null; toast.add({ title: '已解锁，教师可更正后重新发布', color: 'success' }); await loadData() } catch { /* 请求层提示 */ } finally { saving.value = false } }
onMounted(async () => { terms.value = await getTerms(); termId.value = String(useTermStore().currentTermId || terms.value.find(t => t.status === 'ENROLLING')?.id || ''); await loadData() })
</script>
