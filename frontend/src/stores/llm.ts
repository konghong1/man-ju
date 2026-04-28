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

const DEFAULT_PROJECT_ID = "demo";

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });
  if (!response.ok) {
    throw new Error(`API ${path} failed with ${response.status}`);
  }
  return (await response.json()) as T;
}

export const useLlmStore = defineStore("llm", {
  state: (): LlmState => ({
    providers: [],
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
    async refreshConfig(projectId = DEFAULT_PROJECT_ID) {
      try {
        const [providerStates, models, routes] = await Promise.all([
          apiRequest<Record<string, boolean>>("/api/llm/providers"),
          apiRequest<Array<{ provider: string; modelId: string }>>("/api/llm/models"),
          apiRequest<Record<string, string>>(`/api/llm/routes/project/${projectId}`),
        ]);

        const modelByProvider: Record<string, string> = {};
        for (const item of models) {
          if (!modelByProvider[item.provider]) {
            modelByProvider[item.provider] = item.modelId;
          }
        }

        this.providers = Object.entries(providerStates).map(([provider, enabled]) => ({
          key: provider,
          enabled,
          model: modelByProvider[provider] ?? provider,
        }));
        this.routeByStep = { ...routes };
      } catch (error) {
        console.warn("Failed to refresh LLM config", error);
      }
    },
    async setProviderEnabled(key: string, enabled: boolean) {
      const target = this.providers.find((provider) => provider.key === key);
      const previousEnabled = target?.enabled;
      if (target) {
        target.enabled = enabled;
      }
      try {
        const states = await apiRequest<Record<string, boolean>>(`/api/llm/providers/${key}`, {
          method: "PATCH",
          body: JSON.stringify({ enabled }),
        });
        for (const provider of this.providers) {
          if (states[provider.key] !== undefined) {
            provider.enabled = states[provider.key];
          }
        }
      } catch (error) {
        if (target && previousEnabled !== undefined) {
          target.enabled = previousEnabled;
        }
        console.warn(`Failed to update provider ${key}`, error);
      }
    },
    setProviderModel(key: string, model: string) {
      const target = this.providers.find((provider) => provider.key === key);
      if (target) {
        target.model = model;
      }
    },
    async setStepRoute(step: string, providerKey: string, projectId = DEFAULT_PROJECT_ID) {
      const previousRoutes = { ...this.routeByStep };
      this.routeByStep[step] = providerKey;
      try {
        this.routeByStep = await apiRequest<Record<string, string>>(`/api/llm/routes/project/${projectId}`, {
          method: "PUT",
          body: JSON.stringify({ routes: this.routeByStep }),
        });
      } catch (error) {
        this.routeByStep = previousRoutes;
        console.warn("Failed to update step route", error);
      }
    },
  },
});
