package com.iams.audit.api.dto;

import com.iams.audit.domain.ScopeChangeDisposition;
import jakarta.validation.constraints.NotNull;

/** US-AUD-23: resolve a SCOPE_CHANGED finding so it stops blocking closure. */
public record AuditScopeChangeDispositionRequest(
        @NotNull ScopeChangeDisposition disposition
) {
}
