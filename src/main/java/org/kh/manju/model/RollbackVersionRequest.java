package org.kh.manju.model;

import jakarta.validation.constraints.NotBlank;

public record RollbackVersionRequest(
        @NotBlank String versionId
) {
}
