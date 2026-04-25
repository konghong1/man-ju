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

type BackendMetric = {
  provider: string;
  model: string;
  requests: number;
  successRate: number;
  p95LatencyMs: number;
  avgCostUsd: number;
  totalTokens: number;
};

type MetricAggregate = ProviderMetric & {
  requests: number;
  weightedSuccess: number;
  weightedCost: number;
};

export const useMetricsStore = defineStore("metrics", {
  state: (): MetricsState => ({
    providerMetrics: [],
    lastUpdatedAt: null,
  }),
  actions: {
    async refreshMetrics() {
      const to = new Date().toISOString();
      const from = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
      try {
        const response = await fetch(`/api/llm/metrics?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
        if (!response.ok) {
          throw new Error(`metrics api failed: ${response.status}`);
        }
        const payload = (await response.json()) as { metrics: BackendMetric[] };

        const byProviderModel: Record<string, MetricAggregate> = {};
        for (const item of payload.metrics ?? []) {
          const key = `${item.provider}::${item.model}`;
          const existing = byProviderModel[key];
          if (!existing) {
            byProviderModel[key] = {
              provider: item.provider,
              model: item.model,
              p95Ms: item.p95LatencyMs,
              successRate: 0,
              avgCostUsd: 0,
              tokens: item.totalTokens,
              requests: item.requests,
              weightedSuccess: item.successRate * item.requests,
              weightedCost: item.avgCostUsd * item.requests,
            };
            continue;
          }

          existing.p95Ms = Math.max(existing.p95Ms, item.p95LatencyMs);
          existing.tokens += item.totalTokens;
          existing.requests += item.requests;
          existing.weightedSuccess += item.successRate * item.requests;
          existing.weightedCost += item.avgCostUsd * item.requests;
        }

        this.providerMetrics = Object.values(byProviderModel)
          .map((item) => {
            const divisor = item.requests === 0 ? 1 : item.requests;
            return {
              provider: item.provider,
              model: item.model,
              p95Ms: item.p95Ms,
              successRate: Number(((item.weightedSuccess / divisor) * 100).toFixed(2)),
              avgCostUsd: Number((item.weightedCost / divisor).toFixed(6)),
              tokens: item.tokens,
            };
          })
          .sort((a, b) => `${a.provider}:${a.model}`.localeCompare(`${b.provider}:${b.model}`));
        this.lastUpdatedAt = new Date().toISOString();
      } catch (error) {
        console.warn("Failed to refresh metrics", error);
        this.lastUpdatedAt = new Date().toISOString();
      }
    },
  },
});
