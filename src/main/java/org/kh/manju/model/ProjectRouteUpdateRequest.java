package org.kh.manju.model;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ProjectRouteUpdateRequest(
        @NotNull Map<GenerationStep, String> routes
) {
}
