<template>
  <div class="placeholder">
    <h1>{{ title }}</h1>
    <p>{{ description }}</p>
    <div class="todo-hint">
      TODO（待前端模型实现）：{{ hint }}
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 占位页面通用骨架。各路由页面在真正实现前用它占位，并标注该页要做什么。
 * 后续模型开发时直接替换为真实实现。
 */
defineProps<{
  title: string
  description?: string
  hint: string
}>()
</script>
