import { defineStore } from "pinia";

export type ProviderMetric = {
  provider: string;
  model: string;
  p95Ms: number;
  successRate: number;
  avgCostUsd: number;
  tokens: number;
};

type MetricsState = {
  providerMetrics: ProviderMetric[];
  lastUpdatedAt: string | null;
};

export const useMetricsStore = defineStore("metrics", {
  state: (): MetricsState => ({
    providerMetrics: [
      { provider: "openai", model: "gpt-5.2", p95Ms: 1420, successRate: 99.1, avgCostUsd: 0.034, tokens: 12840 },
      {
        provider: "anthropic",
        model: "claude-opus-4.1",
        p95Ms: 1730,
        successRate: 98.8,
        avgCostUsd: 0.041,
        tokens: 10970,
      },
      { provider: "gemini", model: "gemini-2.5-pro", p95Ms: 1505, successRate: 98.2, avgCostUsd: 0.026, tokens: 9220 },
    ],
    lastUpdatedAt: null,
  }),
  actions: {
    refreshMetrics() {
      this.providerMetrics = this.providerMetrics.map((item) => ({
        ...item,
        p95Ms: Math.max(600, item.p95Ms + Math.round(Math.random() * 120) - 60),
        successRate: Number(Math.min(99.9, Math.max(90, item.successRate + (Math.random() * 0.8 - 0.4))).toFixed(2)),
        avgCostUsd: Number(Math.max(0.001, item.avgCostUsd + (Math.random() * 0.006 - 0.003)).toFixed(3)),
        tokens: Math.max(500, item.tokens + Math.round(Math.random() * 1200) - 600),
      }));
      this.lastUpdatedAt = new Date().toISOString();
    },
  },
});
