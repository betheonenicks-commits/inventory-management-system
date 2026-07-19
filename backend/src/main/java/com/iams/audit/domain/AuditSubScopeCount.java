package com.iams.audit.domain;

import java.util.UUID;

/**
 * US-AUD-03: a GROUP BY aggregate row - how many of an audit's expected assets
 * live in one org node (location). The audit's "sub-scopes" are the distinct
 * locations its expected-asset set spans; this is the per-location expected
 * count that seeds the progress breakdown. Populated directly by a JPQL
 * constructor expression, so the DB does the counting (a bulk audit across a
 * wide scope never loads every expected-asset row just to group them).
 */
public record AuditSubScopeCount(UUID orgNodeId, String orgNodeName, String orgNodeCode, long count) {
}
