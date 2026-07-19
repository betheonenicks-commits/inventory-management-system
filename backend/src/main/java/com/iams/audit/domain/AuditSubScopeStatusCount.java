package com.iams.audit.domain;

import java.util.UUID;

/**
 * US-AUD-03: a GROUP BY aggregate row - how many findings of one status fall in
 * one org node (location) within an audit. Combined with {@link AuditSubScopeCount}
 * (expected-per-location) this yields the per-sub-scope progress breakdown. The
 * org node is carried on the row too (not just its id) so a location that has a
 * finding but no expected asset - an OUT_OF_SCOPE find at an un-audited location -
 * still names itself in the breakdown without a second lookup.
 */
public record AuditSubScopeStatusCount(UUID orgNodeId, String orgNodeName, String orgNodeCode,
                                       FindingStatus status, long count) {
}
