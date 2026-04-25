<script setup lang="ts">
const props = defineProps<{
  id: string;
}>();
</script>

<template>
  <section class="view-card">
    <h2>导出页面</h2>
    <p>路由已就绪：`/projects/:id/export`</p>
    <p>当前项目 ID：{{ props.id }}</p>
    <p>后续任务会接入 JSON、Markdown 和 Prompt Pack 导出。</p>
  </section>
</template>
