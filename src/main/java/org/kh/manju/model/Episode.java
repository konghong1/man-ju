package org.kh.manju.model;

import java.util.List;

public record Episode(
        int index,
        String title,
        String summary,
        List<Scene> scenes
) {
}
