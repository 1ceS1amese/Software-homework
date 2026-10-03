<template>
  <div class="flex flex-wrap gap-1.5 items-center">
    <el-tooltip v-if="isFull" content="当前教学班已报满，席位用尽" placement="top">
      <el-tag type="danger" size="small" effect="dark">已满员</el-tag>
    </el-tooltip>
    <el-tooltip v-if="hasConflict" content="与您已选的其他课程在时间或节次上有重叠" placement="top">
      <el-tag type="warning" size="small" effect="dark">时间冲突</el-tag>
    </el-tooltip>
    <el-tooltip v-if="prereqMissing" :content="prereqMissing" placement="top">
      <el-tag type="info" size="small" effect="dark">先修课未满足</el-tag>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  isFull?: boolean
  hasConflict?: boolean
  prereqMissing?: string
}>()
</script>
