<template>
  <UApp>
    <component :is="layoutComponent">
      <router-view :key="`${route.fullPath}:${termStore.currentTermId ?? 'none'}`" />
    </component>
  </UApp>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTermStore } from '@/stores/term'
import { useToast } from '@nuxt/ui/composables'
import { setNoticeHandler } from '@/utils/notify'
import AuthLayout from '@/layouts/AuthLayout.vue'
import AppShell from '@/layouts/AppShell.vue'

const route = useRoute()
const termStore = useTermStore()
const toast = useToast()
setNoticeHandler(notice => toast.add(notice))

const layoutComponent = computed(() => {
  if (route.meta.layout === 'AuthLayout') return AuthLayout
  return AppShell
})
</script>
