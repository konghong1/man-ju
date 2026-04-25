package org.kh.manju.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
        @NotBlank String title,
        @NotBlank String genre,
        @NotBlank String tone,
        @NotBlank String targetAudience,
        @NotNull EpisodeLength episodeLength,
        @NotBlank String premise,
        @NotBlank String protagonist,
        @NotBlank String conflict,
        @NotBlank String visualStyle,
        String language
) {
}
