package com.iams.analytics.domain;

import java.time.Instant;

/** One (role, module) cell of the US-ANL-03 adoption matrix, aggregated in the database. */
public record AdoptionAggregate(String role, String module, long events, long distinctUsers, Instant lastUsed) {
}
