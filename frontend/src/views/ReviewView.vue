<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

type Panel = {
  id: string;
  camera?: string;
  narration?: string;
  dialogue?: string;
  imagePrompt?: string;
};

type Scene = {
  id: string;
  title?: string;
  goal?: string;
  conflictBeat?: string;
  panels?: Panel[];
};

type Project = {
  id: string;
  title?: string;
  synopsis?: string;
  scenes?: Scene[];
};

const props = defineProps<{ id: string }>();
const loading = ref(false);
const error = ref<string | null>(null);
const project = ref<Project | null>(null);

const reviewItems = computed(() => {
  const issues: Array<{ level: "high" | "medium" | "low"; message: string; location: string }> = [];
  const scenes = project.value?.scenes ?? [];

  if (!project.value?.synopsis?.trim()) {
    issues.push({ level: "high", message: "项目缺少 synopsis（故事主线）", location: "Project" });
  }

  scenes.forEach((scene, sceneIndex) => {
    const sceneLabel = scene.title?.trim() ? scene.title : `Scene ${sceneIndex + 1}`;
    if (!scene.goal?.trim()) {
      issues.push({ level: "medium", message: "场景缺少目标（goal）", location: sceneLabel });
    }

    const panels = scene.panels ?? [];
    if (panels.length === 0) {
      issues.push({ level: "high", message: "场景没有分镜 panels", location: sceneLabel });
    }

    panels.forEach((panel, panelIndex) => {
      const panelLabel = `${sceneLabel} / Panel ${panelIndex + 1}`;
      if (!panel.imagePrompt?.trim()) {
        issues.push({ level: "high", message: "分镜缺少 imagePrompt", location: panelLabel });
      }
      if (!panel.dialogue?.trim() && !panel.narration?.trim()) {
        issues.push({ level: "medium", message: "分镜缺少对白与旁白", location: panelLabel });
      }
      if (!panel.camera?.trim()) {
        issues.push({ level: "low", message: "建议补充镜头信息（camera）", location: panelLabel });
      }
    });
  });

  return issues;
});

const qualityScore = computed(() => {
  const base = 100;
  return Math.max(
    0,
    base -
      reviewItems.value.filter((item) => item.level === "high").length * 20 -
      reviewItems.value.filter((item) => item.level === "medium").length * 8 -
      reviewItems.value.filter((item) => item.level === "low").length * 3
  );
});

async function loadProject() {
  loading.value = true;
  error.value = null;
  try {
    const response = await fetch(`/api/projects/${props.id}`);
    if (!response.ok) {
      throw new Error(`加载失败：${response.status}`);
    }
    project.value = await response.json();
  } catch (err) {
    error.value = err instanceof Error ? err.message : "未知错误";
  } finally {
    loading.value = false;
  }
}

onMounted(loadProject);
watch(() => props.id, loadProject);
</script>

<template>
  <section class="view-card">
    <div class="metrics-head">
      <h2>质量评审</h2>
      <button type="button" @click="loadProject">重新评审</button>
    </div>
    <p>当前项目 ID：{{ props.id }}</p>

    <p v-if="loading">正在加载项目并执行规则评审...</p>
    <p v-else-if="error">{{ error }}</p>

    <template v-else>
      <p>质量分：<strong>{{ qualityScore }}</strong> / 100</p>
      <p>问题数：{{ reviewItems.length }}</p>
      <ul v-if="reviewItems.length > 0" class="workspace__list">
        <li v-for="(item, idx) in reviewItems" :key="idx">
          <strong>[{{ item.level.toUpperCase() }}]</strong> {{ item.message }}（{{ item.location }}）
        </li>
      </ul>
      <p v-else>未发现结构性问题，已达到可导出状态。</p>
    </template>
  </section>
</template>
