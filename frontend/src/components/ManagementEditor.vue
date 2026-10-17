<template>
  <UModal v-model:open="open" :title="title" description="请核对后保存；关联关系与业务规则由服务器校验。">
    <template #body><form class="grid gap-4 sm:grid-cols-2" @submit.prevent="$emit('save', { ...model })">
      <label v-for="field in fields" :key="field.key" class="grid min-w-0 gap-1 text-sm">
        <span>{{ field.label }}<span v-if="field.required" aria-hidden="true"> *</span></span>
        <select v-if="field.options" v-model="model[field.key]" :aria-label="field.label" :required="field.required" class="w-full rounded-md border border-(--ui-border) bg-(--ui-bg) p-2">
          <option v-for="option in field.options" :key="String(option.value)" :value="String(option.value)">{{ option.label }}</option>
        </select>
        <input v-else v-model="model[field.key]" :aria-label="field.label" :type="field.type || 'text'" :required="field.required" :min="field.min" :max="field.max" :step="field.step" :maxlength="field.maxlength || 100" class="w-full rounded-md border border-(--ui-border) bg-(--ui-bg) p-2" />
      </label>
      <div class="flex justify-end gap-2 sm:col-span-2"><UButton color="neutral" variant="outline" @click="open = false">取消</UButton><UButton type="submit" :loading="saving" :disabled="saving">保存</UButton></div>
    </form></template>
  </UModal>
</template>
<script setup lang="ts">
import { reactive, watch } from 'vue'
export interface EditorField { key: string; label: string; required?: boolean; type?: string; min?: number; max?: number; step?: string; maxlength?: number; options?: { label: string; value: string | number }[] }
const props = defineProps<{ title: string; fields: EditorField[]; initial: Record<string, unknown>; saving: boolean }>()
const open = defineModel<boolean>('open', { required: true })
defineEmits<{ save: [values: Record<string, string>] }>()
const model = reactive<Record<string, string>>({})
watch(open, value => {
  if (!value) return
  for (const key of Object.keys(model)) delete model[key]
  for (const field of props.fields) model[field.key] = String(props.initial[field.key] ?? '')
})
</script>
