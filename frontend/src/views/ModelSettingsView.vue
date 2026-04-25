<script setup lang="ts">
import { computed } from "vue";
import { onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useLlmStore } from "../stores/llm";
import { useMetricsStore } from "../stores/metrics";

const llmStore = useLlmStore();
const metricsStore = useMetricsStore();

const { providers, routeByStep } = storeToRefs(llmStore);
const { providerMetrics, lastUpdatedAt } = storeToRefs(metricsStore);

const providerOptions = computed(() => providers.value.map((item) => item.key));
const stepKeys = computed(() => Object.keys(routeByStep.value));

onMounted(async () => {
  await llmStore.refreshConfig();
  await metricsStore.refreshMetrics();
});
</script>

<template>
  <section class="view-card">
    <h2>模型设置与指标</h2>
    <p>支持按厂商与模型选择，并按步骤路由。</p>

    <div class="settings-grid">
      <article class="settings-card">
        <h3>Provider 开关与模型</h3>
        <div v-for="provider in providers" :key="provider.key" class="provider-row">
          <label>
            <input
              type="checkbox"
              :checked="provider.enabled"
              @change="llmStore.setProviderEnabled(provider.key, ($event.target as HTMLInputElement).checked)"
            />
            {{ provider.key }}
          </label>
          <input :value="provider.model" @input="llmStore.setProviderModel(provider.key, ($event.target as HTMLInputElement).value)" />
        </div>
      </article>

      <article class="settings-card">
        <h3>步骤路由</h3>
        <div v-for="step in stepKeys" :key="step" class="route-row">
          <span>{{ step }}</span>
          <select :value="routeByStep[step]" @change="llmStore.setStepRoute(step, ($event.target as HTMLSelectElement).value)">
            <option v-for="providerKey in providerOptions" :key="providerKey" :value="providerKey">
              {{ providerKey }}
            </option>
          </select>
        </div>
      </article>
    </div>

    <article class="settings-card settings-card--metrics">
      <div class="metrics-head">
        <h3>运行时指标</h3>
        <button type="button" @click="metricsStore.refreshMetrics">刷新指标</button>
      </div>
      <p>最后更新时间：{{ lastUpdatedAt ?? "未刷新" }}</p>
      <table class="metrics-table">
        <thead>
          <tr>
            <th>Provider</th>
            <th>Model</th>
            <th>P95(ms)</th>
            <th>Success(%)</th>
            <th>Avg Cost($)</th>
            <th>Tokens</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in providerMetrics" :key="item.provider + item.model">
            <td>{{ item.provider }}</td>
            <td>{{ item.model }}</td>
            <td>{{ item.p95Ms }}</td>
            <td>{{ item.successRate }}</td>
            <td>{{ item.avgCostUsd }}</td>
            <td>{{ item.tokens }}</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>
