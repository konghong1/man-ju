package org.kh.manju.model;

public record Panel(
        int index,
        String camera,
        String narration,
        String dialogue,
        String sfx,
        String imagePrompt
) {
}
