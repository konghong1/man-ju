import { defineStore } from "pinia";

type UiState = {
  leftTreeCollapsed: boolean;
  rightPanelCollapsed: boolean;
  lastRoutePath: string;
};

export const useUiStore = defineStore("ui", {
  state: (): UiState => ({
    leftTreeCollapsed: false,
    rightPanelCollapsed: false,
    lastRoutePath: "/",
  }),
  actions: {
    setLeftTreeCollapsed(value: boolean) {
      this.leftTreeCollapsed = value;
    },
    setRightPanelCollapsed(value: boolean) {
      this.rightPanelCollapsed = value;
    },
    setLastRoutePath(path: string) {
      this.lastRoutePath = path;
    },
  },
});
