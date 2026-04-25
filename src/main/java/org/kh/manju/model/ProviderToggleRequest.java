package org.kh.manju.model;

import jakarta.validation.constraints.NotNull;

public record ProviderToggleRequest(
        @NotNull Boolean enabled
) {
}
