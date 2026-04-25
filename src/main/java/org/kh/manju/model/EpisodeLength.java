package org.kh.manju.model;

public enum EpisodeLength {
    SHORT(3),
    MEDIUM(4),
    LONG(6);

    private final int sceneCount;

    EpisodeLength(int sceneCount) {
        this.sceneCount = sceneCount;
    }

    public int sceneCount() {
        return sceneCount;
    }
}
