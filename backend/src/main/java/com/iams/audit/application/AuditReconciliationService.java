package com.iams.audit.application;

import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingReconciliation;
import com.iams.audit.domain.AuditFindingReconciliationRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-21: reconcile a Missing finding found later, outside any active
 * audit - the original {@link AuditFinding} is never touched (AC-AUD-21-X);
 * only a new linked {@link AuditFindingReconciliation} row is ever written.
 * Deliberately not scoped to the finding's own audit being IN_PROGRESS - by
 * definition a Missing finding only exists on a submitted (usually closed)
 * audit, so requiring it be reconciled "outside an active audit" is the
 * normal case here, not an edge case to special-case around.
 */
@Service
public class AuditReconciliationService {

    private static final String FALLBACK_STATUS_CODE = "IN_STORAGE";

    private final AuditFindingRepository findingRepository;
    private final AuditFindingReconciliationRepository reconciliationRepository;
    private final AssetRepository assetRepository;
    private final AssetStatusDefRepository statusDefRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;

    public AuditReconciliationService(AuditFindingRepository findingRepository,
                                       AuditFindingReconciliationRepository reconciliationRepository,
                                       AssetRepository assetRepository, AssetStatusDefRepository statusDefRepository,
                                       AssetHistoryRecorder historyRecorder, CurrentUserProvider currentUserProvider) {
        this.findingRepository = findingRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.assetRepository = assetRepository;
        this.statusDefRepository = statusDefRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public AuditFindingReconciliation reconcile(UUID auditId, UUID findingId, String foundLocationNote) {
        if (foundLocationNote == null || foundLocationNote.isBlank()) {
            throw ValidationFailedException.singleField("foundLocationNote",
                    "A note describing where/how the asset was found is required");
        }
        AuditFinding finding = findingRepository.findByIdWithAsset(findingId)
                .orElseThrow(() -> NotFoundException.of("AuditFinding", findingId));
        if (!finding.getAudit().getId().equals(auditId)) {
            throw NotFoundException.of("AuditFinding", findingId);
        }
        // AC-AUD-21-X: reconciliation only ever targets a Missing finding - never a Verified/OutOfScope/ScopeChanged one.
        if (finding.getStatus() != FindingStatus.MISSING) {
            throw new ConflictException("FINDING_NOT_MISSING", "Only a finding classified Missing can be reconciled");
        }
        if (reconciliationRepository.findByFindingId(findingId).isPresent()) {
            throw new ConflictException("ALREADY_RECONCILED", "This Missing finding has already been reconciled");
        }

        CurrentUser actor = currentUserProvider.current();
        AuditFindingReconciliation reconciliation = new AuditFindingReconciliation();
        reconciliation.setFinding(finding);
        reconciliation.setFoundLocationNote(foundLocationNote);
        reconciliation.setReconciledByUserId(actor.id());
        reconciliation.setReconciledByUsername(actor.username());
        reconciliation = reconciliationRepository.save(reconciliation);

        // AC-AUD-21-H: "updates the asset's status" - reverted to exactly where it was
        // before this finding flipped it to MISSING, not a hardcoded fallback (mirrors
        // RepairService.close()'s own previousStatusCode-revert discipline from EPIC-LIF).
        String restoreStatusCode = finding.getPreviousStatusCode() != null ? finding.getPreviousStatusCode() : FALLBACK_STATUS_CODE;
        AssetStatusDef restoreStatus = statusDefRepository.findByCode(restoreStatusCode)
                .orElseThrow(() -> new IllegalStateException(restoreStatusCode + " status missing from seed data"));

        Asset asset = finding.getAsset();
        String currentStatusCode = asset.getStatus() != null ? asset.getStatus().getCode() : null;
        asset.setStatus(restoreStatus);
        asset.setUpdatedBy(actor.id());
        asset = assetRepository.saveAndFlush(asset);
        historyRecorder.record(asset, AssetHistoryEventType.STATUS_CHANGE, "status", currentStatusCode,
                restoreStatusCode + " (reconciled from Missing: " + foundLocationNote + ")");

        return reconciliation;
    }

    /** Null when the finding hasn't been reconciled - lets a response embed "has this been resolved?" without a client guessing from a blind 409. */
    @Transactional(readOnly = true)
    public AuditFindingReconciliation forFinding(UUID findingId) {
        return reconciliationRepository.findByFindingId(findingId).orElse(null);
    }
}
