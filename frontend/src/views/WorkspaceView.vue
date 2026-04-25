<script setup lang="ts">
const props = defineProps<{
  id: string;
}>();
</script>

<template>
  <section class="view-card">
    <h2>项目工作台</h2>
    <p>路由已就绪：`/projects/:id/workspace`</p>
    <p>当前项目 ID：{{ props.id }}</p>
    <p>后续任务会补充三栏布局、场景编辑器和分镜编辑器。</p>
  </section>
</template>
