import { defineStore } from "pinia";

export type DraftPanel = {
  id: string;
  camera: string;
  narration: string;
  dialogue: string;
  sfx: string;
  imagePrompt: string;
};

export type DraftScene = {
  id: string;
  title: string;
  goal: string;
  conflictBeat: string;
  panels: DraftPanel[];
};

export type ProjectDraft = {
  projectId: string;
  scenes: DraftScene[];
  updatedAt: string;
};

type WorkspaceState = {
  draftsByProject: Record<string, ProjectDraft>;
};

function newPanel(sceneId: string, index: number): DraftPanel {
  return {
    id: `${sceneId}-panel-${index}`,
    camera: "medium shot",
    narration: `Scene ${sceneId} panel ${index} narration`,
    dialogue: "“继续推进计划。”",
    sfx: "Tap",
    imagePrompt: "comic panel, cinematic framing, high contrast",
  };
}

function newScene(index: number): DraftScene {
  const id = `scene-${index}`;
  return {
    id,
    title: `Scene ${index}`,
    goal: "推进剧情目标",
    conflictBeat: "主角与阻力正面碰撞",
    panels: [newPanel(id, 1), newPanel(id, 2)],
  };
}

export const useWorkspaceStore = defineStore("workspace", {
  state: (): WorkspaceState => ({
    draftsByProject: {},
  }),
  actions: {
    ensureDraft(projectId: string) {
      if (this.draftsByProject[projectId]) {
        return;
      }

      this.draftsByProject[projectId] = {
        projectId,
        scenes: [newScene(1), newScene(2)],
        updatedAt: new Date().toISOString(),
      };
    },
    setSceneField(projectId: string, sceneId: string, field: "title" | "goal" | "conflictBeat", value: string) {
      const scene = this.draftsByProject[projectId]?.scenes.find((item) => item.id === sceneId);
      if (!scene) {
        return;
      }
      scene[field] = value;
      this.touch(projectId);
    },
    setPanelField(
      projectId: string,
      sceneId: string,
      panelId: string,
      field: "camera" | "narration" | "dialogue" | "sfx" | "imagePrompt",
      value: string
    ) {
      const scene = this.draftsByProject[projectId]?.scenes.find((item) => item.id === sceneId);
      const panel = scene?.panels.find((item) => item.id === panelId);
      if (!panel) {
        return;
      }
      panel[field] = value;
      this.touch(projectId);
    },
    addScene(projectId: string) {
      const draft = this.draftsByProject[projectId];
      if (!draft) {
        return;
      }
      draft.scenes.push(newScene(draft.scenes.length + 1));
      this.touch(projectId);
    },
    addPanel(projectId: string, sceneId: string) {
      const scene = this.draftsByProject[projectId]?.scenes.find((item) => item.id === sceneId);
      if (!scene) {
        return;
      }
      scene.panels.push(newPanel(sceneId, scene.panels.length + 1));
      this.touch(projectId);
    },
    touch(projectId: string) {
      const draft = this.draftsByProject[projectId];
      if (draft) {
        draft.updatedAt = new Date().toISOString();
      }
    },
  },
});
