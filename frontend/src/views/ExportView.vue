<script setup lang="ts">
import { ref } from "vue";

const props = defineProps<{ id: string }>();
const format = ref<"json" | "markdown">("json");
const loading = ref(false);
const error = ref<string | null>(null);
const output = ref("");
const copyState = ref<"idle" | "success" | "failed">("idle");

async function runExport() {
  loading.value = true;
  error.value = null;
  output.value = "";
  try {
    const response = await fetch(`/api/projects/${props.id}/export?format=${format.value}`);
    if (!response.ok) {
      throw new Error(`导出失败：${response.status}`);
    }
    output.value = await response.text();
  } catch (err) {
    error.value = err instanceof Error ? err.message : "未知错误";
  } finally {
    loading.value = false;
  }
}

async function copyOutput() {
  copyState.value = "idle";
  if (!output.value) {
    return;
  }

  try {
    if (!navigator.clipboard?.writeText) {
      throw new Error("clipboard unavailable");
    }
    await navigator.clipboard.writeText(output.value);
    copyState.value = "success";
  } catch {
    copyState.value = "failed";
  }
}
</script>

<template>
  <section class="view-card">
    <h2>导出页面</h2>
    <p>当前项目 ID：{{ props.id }}</p>
    <div class="route-row">
      <span>导出格式</span>
      <select v-model="format">
        <option value="json">JSON</option>
        <option value="markdown">Markdown</option>
      </select>
      <button type="button" @click="runExport">执行导出</button>
      <button type="button" :disabled="!output" @click="copyOutput">复制结果</button>
    </div>
    <p v-if="copyState === 'success'">复制成功。</p>
    <p v-else-if="copyState === 'failed'">复制失败，请手动复制。</p>

    <p v-if="loading">导出中...</p>
    <p v-else-if="error">{{ error }}</p>
    <pre v-else-if="output" style="white-space: pre-wrap; max-height: 360px; overflow: auto">{{ output }}</pre>
    <p v-else>请选择格式并执行导出。</p>
  </section>
</template>
