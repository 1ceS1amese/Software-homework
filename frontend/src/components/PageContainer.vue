<template>
  <div class="min-w-0 space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-(--ui-border) bg-(--ui-bg) p-5 sm:p-6">
      <div>
        <h1 class="text-xl font-semibold tracking-tight text-(--ui-text-highlighted) sm:text-2xl">{{ title }}</h1>
        <p v-if="description" class="mt-1 text-sm text-(--ui-text-muted)">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="flex flex-wrap items-center gap-3">
        <slot name="actions" />
      </div>
    </div>

    <div class="min-w-0">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>
