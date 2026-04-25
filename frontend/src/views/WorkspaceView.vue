<script setup lang="ts">
import { computed, ref, watchEffect } from "vue";
import { storeToRefs } from "pinia";
import { useWorkspaceStore } from "../stores/workspace";

const props = defineProps<{
  id: string;
}>();

const workspaceStore = useWorkspaceStore();
workspaceStore.ensureDraft(props.id);

const { draftsByProject } = storeToRefs(workspaceStore);
const selectedSceneId = ref<string>("");
const selectedPanelId = ref<string>("");

const draft = computed(() => draftsByProject.value[props.id]);
const scenes = computed(() => draft.value?.scenes ?? []);
const selectedScene = computed(() => scenes.value.find((scene) => scene.id === selectedSceneId.value) ?? null);
const selectedPanel = computed(
  () => selectedScene.value?.panels.find((panel) => panel.id === selectedPanelId.value) ?? null
);

watchEffect(() => {
  if (!selectedSceneId.value && scenes.value.length > 0) {
    selectedSceneId.value = scenes.value[0].id;
  }
});

watchEffect(() => {
  if (selectedScene.value && !selectedPanelId.value && selectedScene.value.panels.length > 0) {
    selectedPanelId.value = selectedScene.value.panels[0].id;
  }
});

watchEffect(() => {
  if (selectedScene.value && selectedPanelId.value) {
    const exists = selectedScene.value.panels.some((panel) => panel.id === selectedPanelId.value);
    if (!exists) {
      selectedPanelId.value = selectedScene.value.panels[0]?.id ?? "";
    }
  }
});
</script>

<template>
  <section class="view-card workspace">
    <header class="workspace__header">
      <h2>项目工作台</h2>
      <p>当前项目 ID：{{ props.id }}</p>
      <p>最后更新时间：{{ draft?.updatedAt ?? "-" }}</p>
    </header>

    <div class="workspace__grid">
      <aside class="workspace__col">
        <div class="workspace__title-row">
          <h3>Scenes</h3>
          <button type="button" @click="workspaceStore.addScene(props.id)">新增 Scene</button>
        </div>
        <ul class="workspace__list">
          <li v-for="scene in scenes" :key="scene.id">
            <button type="button" :class="{ active: scene.id === selectedSceneId }" @click="selectedSceneId = scene.id">
              {{ scene.title }}
            </button>
          </li>
        </ul>
      </aside>

      <article class="workspace__col" v-if="selectedScene">
        <h3>Scene Editor</h3>
        <label>
          标题
          <input
            :value="selectedScene.title"
            @input="workspaceStore.setSceneField(props.id, selectedScene.id, 'title', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label>
          目标
          <textarea
            rows="2"
            :value="selectedScene.goal"
            @input="workspaceStore.setSceneField(props.id, selectedScene.id, 'goal', ($event.target as HTMLTextAreaElement).value)"
          />
        </label>
        <label>
          冲突
          <textarea
            rows="2"
            :value="selectedScene.conflictBeat"
            @input="
              workspaceStore.setSceneField(
                props.id,
                selectedScene.id,
                'conflictBeat',
                ($event.target as HTMLTextAreaElement).value
              )
            "
          />
        </label>
      </article>

      <article class="workspace__col" v-if="selectedScene">
        <div class="workspace__title-row">
          <h3>Panel Editor</h3>
          <button type="button" @click="workspaceStore.addPanel(props.id, selectedScene.id)">新增 Panel</button>
        </div>
        <ul class="workspace__list panel-list">
          <li v-for="panel in selectedScene.panels" :key="panel.id">
            <button
              type="button"
              :class="{ active: panel.id === selectedPanelId }"
              @click="selectedPanelId = panel.id"
            >
              {{ panel.id }}
            </button>
          </li>
        </ul>

        <div v-if="selectedPanel" class="workspace__form">
          <label>
            镜头
            <input
              :value="selectedPanel.camera"
              @input="
                workspaceStore.setPanelField(
                  props.id,
                  selectedScene.id,
                  selectedPanel.id,
                  'camera',
                  ($event.target as HTMLInputElement).value
                )
              "
            />
          </label>
          <label>
            旁白
            <textarea
              rows="2"
              :value="selectedPanel.narration"
              @input="
                workspaceStore.setPanelField(
                  props.id,
                  selectedScene.id,
                  selectedPanel.id,
                  'narration',
                  ($event.target as HTMLTextAreaElement).value
                )
              "
            />
          </label>
          <label>
            对白
            <textarea
              rows="2"
              :value="selectedPanel.dialogue"
              @input="
                workspaceStore.setPanelField(
                  props.id,
                  selectedScene.id,
                  selectedPanel.id,
                  'dialogue',
                  ($event.target as HTMLTextAreaElement).value
                )
              "
            />
          </label>
          <label>
            音效
            <input
              :value="selectedPanel.sfx"
              @input="
                workspaceStore.setPanelField(
                  props.id,
                  selectedScene.id,
                  selectedPanel.id,
                  'sfx',
                  ($event.target as HTMLInputElement).value
                )
              "
            />
          </label>
          <label>
            图像提示词
            <textarea
              rows="3"
              :value="selectedPanel.imagePrompt"
              @input="
                workspaceStore.setPanelField(
                  props.id,
                  selectedScene.id,
                  selectedPanel.id,
                  'imagePrompt',
                  ($event.target as HTMLTextAreaElement).value
                )
              "
            />
          </label>
        </div>
      </article>
    </div>
  </section>
</template>
