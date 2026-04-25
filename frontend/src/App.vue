<script setup lang="ts">
import { storeToRefs } from "pinia";
import { watch } from "vue";
import { useRoute } from "vue-router";
import { useUiStore } from "./stores/ui";

const route = useRoute();
const uiStore = useUiStore();
const { leftTreeCollapsed, rightPanelCollapsed } = storeToRefs(uiStore);

watch(
  () => route.fullPath,
  (path) => {
    uiStore.setLastRoutePath(path);
  },
  { immediate: true }
);
</script>

<template>
  <div class="shell">
    <header class="shell__header">
      <h1>Man-Ju Console</h1>
      <div class="shell__switches">
        <button type="button" @click="uiStore.setLeftTreeCollapsed(!leftTreeCollapsed)">
          左侧栏: {{ leftTreeCollapsed ? "收起" : "展开" }}
        </button>
        <button type="button" @click="uiStore.setRightPanelCollapsed(!rightPanelCollapsed)">
          右侧栏: {{ rightPanelCollapsed ? "收起" : "展开" }}
        </button>
      </div>
      <nav class="shell__nav">
        <RouterLink to="/">Projects</RouterLink>
        <RouterLink to="/projects/new">New</RouterLink>
        <RouterLink to="/projects/demo/workspace">Workspace</RouterLink>
        <RouterLink to="/projects/demo/review">Review</RouterLink>
        <RouterLink to="/projects/demo/export">Export</RouterLink>
        <RouterLink to="/settings/models">Models</RouterLink>
      </nav>
    </header>
    <main class="shell__content">
      <RouterView />
    </main>
  </div>
</template>
