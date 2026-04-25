package org.kh.manju.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manju")
public class ManJuProperties {

    private String storageDir = "data/projects";
    private String jobStorageDir = "data/jobs";
    private String storageMode = "file";
    private int panelsPerScene = 3;
    private int llmCircuitFailureThreshold = 2;
    private long llmCircuitOpenMillis = 30_000L;
    private int llmRateLimitMaxRequests = 100;
    private long llmRateLimitWindowMillis = 1_000L;
    private double llmProjectBudgetUsd = 100.0;

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public int getPanelsPerScene() {
        return panelsPerScene;
    }

    public void setPanelsPerScene(int panelsPerScene) {
        this.panelsPerScene = panelsPerScene;
    }

    public String getJobStorageDir() {
        return jobStorageDir;
    }

    public void setJobStorageDir(String jobStorageDir) {
        this.jobStorageDir = jobStorageDir;
    }

    public String getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(String storageMode) {
        this.storageMode = storageMode;
    }

    public int getLlmCircuitFailureThreshold() {
        return llmCircuitFailureThreshold;
    }

    public void setLlmCircuitFailureThreshold(int llmCircuitFailureThreshold) {
        this.llmCircuitFailureThreshold = llmCircuitFailureThreshold;
    }

    public long getLlmCircuitOpenMillis() {
        return llmCircuitOpenMillis;
    }

    public void setLlmCircuitOpenMillis(long llmCircuitOpenMillis) {
        this.llmCircuitOpenMillis = llmCircuitOpenMillis;
    }

    public int getLlmRateLimitMaxRequests() {
        return llmRateLimitMaxRequests;
    }

    public void setLlmRateLimitMaxRequests(int llmRateLimitMaxRequests) {
        this.llmRateLimitMaxRequests = llmRateLimitMaxRequests;
    }

    public long getLlmRateLimitWindowMillis() {
        return llmRateLimitWindowMillis;
    }

    public void setLlmRateLimitWindowMillis(long llmRateLimitWindowMillis) {
        this.llmRateLimitWindowMillis = llmRateLimitWindowMillis;
    }

    public double getLlmProjectBudgetUsd() {
        return llmProjectBudgetUsd;
    }

    public void setLlmProjectBudgetUsd(double llmProjectBudgetUsd) {
        this.llmProjectBudgetUsd = llmProjectBudgetUsd;
    }
}
