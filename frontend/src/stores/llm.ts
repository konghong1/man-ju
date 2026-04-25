import { defineStore } from "pinia";

export type ProviderConfig = {
  key: string;
  enabled: boolean;
  model: string;
};

type LlmState = {
  providers: ProviderConfig[];
  routeByStep: Record<string, string>;
};

export const useLlmStore = defineStore("llm", {
  state: (): LlmState => ({
    providers: [
      { key: "openai", enabled: true, model: "gpt-5.2" },
      { key: "anthropic", enabled: true, model: "claude-opus-4.1" },
      { key: "gemini", enabled: false, model: "gemini-2.5-pro" },
    ],
    routeByStep: {
      S2_STORY_PLAN: "openai",
      S3_SCENE_WRITE: "anthropic",
      S5_PROMPT_COMPILE: "openai",
    },
  }),
  getters: {
    enabledProviderKeys(state) {
      return state.providers.filter((provider) => provider.enabled).map((provider) => provider.key);
    },
  },
  actions: {
    setProviderEnabled(key: string, enabled: boolean) {
      const target = this.providers.find((provider) => provider.key === key);
      if (target) {
        target.enabled = enabled;
      }
    },
    setProviderModel(key: string, model: string) {
      const target = this.providers.find((provider) => provider.key === key);
      if (target) {
        target.model = model;
      }
    },
    setStepRoute(step: string, providerKey: string) {
      this.routeByStep[step] = providerKey;
    },
  },
});
