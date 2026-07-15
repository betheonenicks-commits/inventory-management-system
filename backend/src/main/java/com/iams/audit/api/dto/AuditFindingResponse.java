package com.iams.audit.api.dto;

import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.FindingStatus;
import com.iams.audit.domain.ScopeChangeDisposition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * condition/remarks are the finding's current *effective* value (original,
 * or its latest correction if one exists - see AuditFindingCorrectionService)
 * - not necessarily what was originally recorded. corrections carries the
 * full history so a client can show both, per US-AUD-24's "remains unchanged
 * and visible alongside it."
 * <p>
 * reconciliation is null unless this finding is MISSING and has actually been
 * reconciled (US-AUD-21) - a client uses its presence to know whether to show
 * a "Reconcile" action or the recorded outcome, without needing a separate
 * lookup or guessing from a 409 on a blind attempt.
 */
public record AuditFindingResponse(
        UUID id,
        UUID auditId,
        UUID assetId,
        String assetNumber,
        String assetName,
        FindingStatus status,
        AssetCondition condition,
        String remarks,
        UUID verifiedByUserId,
        String verifiedByUsername,
        Instant verifiedAt,
        String deviceId,
        ScopeChangeDisposition scopeChangeDisposition,
        List<AuditFindingCorrectionResponse> corrections,
        AuditFindingReconciliationResponse reconciliation
) {
}
