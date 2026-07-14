package com.iams.audit.domain;

/**
 * US-AUD-01/09/13/14/15: the audit lifecycle used across this module. This
 * codebase folds the FRS's separate "close scanning" and "close after
 * approval" language into two steps rather than three: submitting an audit
 * (US-AUD-13) both classifies unverified expected assets as Missing
 * (US-AUD-09) and moves it to PENDING_APPROVAL, since the source stories
 * never describe an intermediate state between "still scanning" and
 * "submitted for approval." Approval (US-AUD-14) and closure (US-AUD-15) are
 * the same transactional event here - the stories describe them as one
 * moment ("an approved and closed audit") - so CLOSED is the single terminal
 * status; approvedBy/approvedAt on {@link Audit} record who approved it.
 * Rejecting a submitted audit resets status back to IN_PROGRESS rather than
 * introducing a dead-end REJECTED status.
 */
public enum AuditStatus {
    IN_PROGRESS,
    PENDING_APPROVAL,
    CLOSED
}
