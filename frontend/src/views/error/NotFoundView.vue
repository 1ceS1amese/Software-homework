<template>
  <div class="min-h-screen bg-slate-50 flex items-center justify-center p-6 text-center">
    <div class="max-w-md bg-white p-8 rounded-2xl shadow-sm border border-slate-200 space-y-4">
      <div class="text-6xl">🔍</div>
      <h1 class="text-2xl font-bold text-slate-900">404 页面未找到</h1>
      <p class="text-sm text-slate-500">
        您访问的页面路径不存在，可能已被移动或删除。
      </p>
      <div class="pt-4">
        <UButton to="/">返回首页</UButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>
