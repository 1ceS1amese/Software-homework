<template>
  <div class="py-12 flex flex-col items-center justify-center text-center">
    <div class="text-4xl mb-3">{{ icon || '📭' }}</div>
    <div class="text-sm font-medium text-slate-700">{{ text || '暂无数据' }}</div>
    <p v-if="subText" class="text-xs text-slate-400 mt-1 max-w-sm">{{ subText }}</p>
    <div v-if="$slots.action" class="mt-4">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  text?: string
  subText?: string
}>()
</script>
