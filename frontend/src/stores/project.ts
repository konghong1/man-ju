import { defineStore } from "pinia";

export type ProjectSummary = {
  id: string;
  title: string;
  status: "draft" | "running" | "ready";
};

type ProjectState = {
  items: ProjectSummary[];
  currentProjectId: string | null;
};

export const useProjectStore = defineStore("project", {
  state: (): ProjectState => ({
    items: [],
    currentProjectId: null,
  }),
  getters: {
    currentProject(state) {
      return state.items.find((item) => item.id === state.currentProjectId) ?? null;
    },
  },
  actions: {
    setProjects(items: ProjectSummary[]) {
      this.items = items;
      if (!this.currentProjectId && items.length > 0) {
        this.currentProjectId = items[0].id;
      }
    },
    setCurrentProjectId(projectId: string | null) {
      this.currentProjectId = projectId;
    },
  },
});
