<script setup lang="ts">
const props = defineProps<{
  id: string;
}>();
</script>

<template>
  <section class="view-card">
    <h2>质量评审</h2>
    <p>路由已就绪：`/projects/:id/review`</p>
    <p>当前项目 ID：{{ props.id }}</p>
    <p>后续任务会接入质量报告、问题定位和跳转。</p>
  </section>
</template>
