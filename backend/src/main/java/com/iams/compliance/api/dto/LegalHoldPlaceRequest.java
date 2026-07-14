package com.iams.compliance.api.dto;

import com.iams.compliance.domain.LegalHoldScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LegalHoldPlaceRequest(@NotNull LegalHoldScopeType scopeType, @NotNull UUID scopeId, @NotBlank String reason) {
}
