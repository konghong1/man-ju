package org.kh.manju.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manju")
public class ManJuProperties {

    private String storageDir = "data/projects";
    private int panelsPerScene = 3;

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
}
