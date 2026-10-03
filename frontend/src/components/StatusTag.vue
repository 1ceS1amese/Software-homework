<template>
  <UBadge :color="tagType" size="sm" variant="subtle">
    {{ tagText }}
  </UBadge>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  category?: 'class' | 'term' | 'enroll' | 'grade' | 'user'
}>()

const statusMap: Record<string, { text: string; type: 'success' | 'warning' | 'info' | 'error' | 'primary' | 'neutral' }> = {
  // Teaching class
  DRAFT: { text: '草稿', type: 'neutral' },
  PUBLISHED: { text: '已发布', type: 'success' },
  CLOSED: { text: '已结课', type: 'warning' },
  CANCELLED: { text: '已取消', type: 'error' },

  // Term
  PLANNED: { text: '筹划中', type: 'neutral' },
  ENROLLING: { text: '选课中', type: 'success' },
  RUNNING: { text: '授课进行中', type: 'primary' },

  // Enroll
  ENROLLED: { text: '已选入', type: 'success' },
  WITHDRAWN: { text: '已退选', type: 'error' },
  COMPLETED: { text: '已修完', type: 'primary' },

  // User
  ACTIVE: { text: '正常', type: 'success' },
  DISABLED: { text: '停用', type: 'error' },
}

const tagType = computed(() => {
  return statusMap[props.status]?.type || 'neutral'
})

const tagText = computed(() => {
  return statusMap[props.status]?.text || props.status
})
</script>
