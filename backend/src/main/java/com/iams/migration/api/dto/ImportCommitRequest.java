package com.iams.migration.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * US-MIG-03 AC-H: commit is called with an idempotency key so a replayed commit
 * returns the cached result instead of duplicating. The client generates a stable
 * key (e.g. a UUID) per intended commit and reuses it on retry.
 */
public record ImportCommitRequest(
        @NotBlank String idempotencyKey
) {
}
