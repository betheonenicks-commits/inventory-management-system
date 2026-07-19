package com.iams.audit.api.mapper;

import com.iams.audit.api.dto.AuditAssignmentResponse;
import com.iams.audit.api.dto.AuditBatchScanResponse;
import com.iams.audit.api.dto.AuditCertificateResponse;
import com.iams.audit.api.dto.AuditCycleTrendResponse;
import com.iams.audit.api.dto.AuditDashboardItemResponse;
import com.iams.audit.api.dto.AuditExceptionReportResponse;
import com.iams.audit.api.dto.AuditFindingCorrectionResponse;
import com.iams.audit.api.dto.AuditFindingReconciliationResponse;
import com.iams.audit.api.dto.AuditFindingResponse;
import com.iams.audit.api.dto.AuditProgressResponse;
import com.iams.audit.api.dto.AuditResponse;
import com.iams.audit.api.dto.AuditSubScopeProgressResponse;
import com.iams.audit.api.dto.AuditSummaryResponse;
import com.iams.audit.application.AuditAnalyticsService;
import com.iams.audit.application.AuditFindingCorrectionService;
import com.iams.audit.application.AuditReportService;
import com.iams.audit.application.AuditScanService;
import com.iams.audit.application.AuditService;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingCorrection;
import com.iams.audit.domain.AuditFindingReconciliation;
import com.iams.audit.domain.CorrectionField;
import com.iams.audit.domain.FindingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuditMapper {

    public AuditResponse toResponse(Audit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getName(),
                audit.getAuditType(),
                audit.getScopeOrgNode() != null ? audit.getScopeOrgNode().getId() : null,
                audit.getScopeOrgNode() != null ? audit.getScopeOrgNode().getName() : null,
                audit.getScopeCategory() != null ? audit.getScopeCategory().getId() : null,
                audit.getScopeCategory() != null ? audit.getScopeCategory().getName() : null,
                audit.getStatus(),
                audit.getNominalApproverId(),
                audit.getEffectiveApproverId(),
                audit.getSubmittedBy(),
                audit.getSubmittedAt(),
                audit.getSignatureName(),
                audit.getApprovedBy(),
                audit.getApprovedAt(),
                audit.getLastRejectionReason(),
                audit.getScheduledDate(),
                audit.getVersion()
        );
    }

    public AuditSummaryResponse toSummary(Audit audit) {
        return new AuditSummaryResponse(audit.getId(), audit.getName());
    }

    public AuditAssignmentResponse toResponse(AuditAssignment assignment) {
        return new AuditAssignmentResponse(
                assignment.getId(),
                assignment.getAudit().getId(),
                assignment.getAuditorUserId(),
                assignment.getAuditorUsername(),
                assignment.getSubScope(),
                assignment.isActive(),
                assignment.getUnassignedAt(),
                assignment.getVersion()
        );
    }

    /** Freshly-scanned/batch-scanned findings can never already be reconciled (US-AUD-21 only applies to a system-classified Missing finding), so reconciliation is unconditionally null here. */
    public AuditFindingResponse toResponse(AuditFinding finding, List<AuditFindingCorrection> correctionsAscending) {
        return toResponse(finding, correctionsAscending, null);
    }

    public AuditFindingResponse toResponse(AuditFinding finding, List<AuditFindingCorrection> correctionsAscending,
                                            AuditFindingReconciliation reconciliation) {
        String effectiveCondition = AuditFindingCorrectionService.effectiveValue(finding, CorrectionField.CONDITION, correctionsAscending);
        String effectiveRemarks = AuditFindingCorrectionService.effectiveValue(finding, CorrectionField.REMARKS, correctionsAscending);
        return new AuditFindingResponse(
                finding.getId(),
                finding.getAudit().getId(),
                finding.getAsset().getId(),
                finding.getAsset().getAssetNumber(),
                finding.getAsset().getName(),
                finding.getStatus(),
                effectiveCondition != null ? com.iams.audit.domain.AssetCondition.valueOf(effectiveCondition) : null,
                effectiveRemarks,
                finding.getVerifiedByUserId(),
                finding.getVerifiedByUsername(),
                finding.getVerifiedAt(),
                finding.getDeviceId(),
                finding.getScopeChangeDisposition(),
                correctionsAscending.stream().map(this::toResponse).toList(),
                reconciliation != null ? toResponse(reconciliation) : null
        );
    }

    public AuditFindingReconciliationResponse toResponse(AuditFindingReconciliation reconciliation) {
        return new AuditFindingReconciliationResponse(
                reconciliation.getId(),
                reconciliation.getFinding().getId(),
                reconciliation.getFoundLocationNote(),
                reconciliation.getReconciledByUserId(),
                reconciliation.getReconciledByUsername(),
                reconciliation.getReconciledAt()
        );
    }

    public AuditFindingCorrectionResponse toResponse(AuditFindingCorrection correction) {
        return new AuditFindingCorrectionResponse(
                correction.getId(),
                correction.getFieldName(),
                correction.getOldValue(),
                correction.getNewValue(),
                correction.getActorId(),
                correction.getActorUsername(),
                correction.getCreatedAt()
        );
    }

    /** Flat totals only (dashboard tiles) - no sub-scope breakdown carried. */
    public AuditProgressResponse toResponse(AuditService.AuditProgress progress) {
        return progressResponse(progress, java.util.List.of());
    }

    /** US-AUD-03: flat totals plus the per-sub-scope (per-location) breakdown, for the audit-detail view. */
    public AuditProgressResponse toResponse(AuditService.AuditProgressDetail detail) {
        List<AuditSubScopeProgressResponse> subScopes = detail.subScopes().stream()
                .map(this::toResponse)
                .toList();
        return progressResponse(detail.totals(), subScopes);
    }

    private AuditProgressResponse progressResponse(AuditService.AuditProgress progress,
                                                   List<AuditSubScopeProgressResponse> subScopes) {
        return new AuditProgressResponse(
                progress.expectedCount(),
                progress.verifiedCount(),
                progress.missingCount(),
                progress.outOfScopeCount(),
                progress.scopeChangedCount(),
                percentComplete(progress.verifiedCount(), progress.expectedCount()),
                subScopes
        );
    }

    private AuditSubScopeProgressResponse toResponse(AuditService.SubScopeProgress sub) {
        return new AuditSubScopeProgressResponse(
                sub.orgNodeId(),
                sub.orgNodeName(),
                sub.orgNodeCode(),
                sub.expectedCount(),
                sub.verifiedCount(),
                sub.missingCount(),
                sub.outOfScopeCount(),
                sub.scopeChangedCount(),
                percentComplete(sub.verifiedCount(), sub.expectedCount())
        );
    }

    private double percentComplete(long verified, long expected) {
        double percent = expected == 0 ? 0.0 : (verified * 100.0) / expected;
        return Math.round(percent * 10) / 10.0;
    }

    /** US-AUD-18: cross-cycle trend rows. */
    public List<AuditCycleTrendResponse> toCycleTrendResponse(List<AuditAnalyticsService.AuditCycleTrend> cycles) {
        return cycles.stream()
                .map(c -> new AuditCycleTrendResponse(
                        c.auditId(), c.name(), c.approvedAt(),
                        c.expectedCount(), c.missingCount(), c.reconciledCount(), c.netMissingCount(),
                        c.missingRatePct(), c.netMissingRatePct(), c.completionDays()))
                .toList();
    }

    public AuditCertificateResponse toResponse(AuditReportService.AuditCertificate certificate) {
        return new AuditCertificateResponse(
                certificate.auditId(),
                certificate.auditName(),
                certificate.expectedCount(),
                certificate.verifiedCount(),
                certificate.missingCount(),
                certificate.damagedCount(),
                certificate.approvedBy(),
                certificate.approverName(),
                certificate.approvedAt()
        );
    }

    public AuditDashboardItemResponse toResponse(AuditReportService.AuditDashboardItem item) {
        return new AuditDashboardItemResponse(
                item.auditId(),
                item.name(),
                item.status(),
                toResponse(item.progress()),
                item.exceptionCount()
        );
    }

    public AuditExceptionReportResponse toExceptionReport(UUID auditId, List<AuditFinding> findings,
                                                            java.util.function.Function<UUID, List<AuditFindingCorrection>> correctionsLookup,
                                                            java.util.function.Function<UUID, AuditFindingReconciliation> reconciliationLookup) {
        List<AuditFindingResponse> mapped = findings.stream()
                .map(f -> toResponse(f, correctionsLookup.apply(f.getId()), reconciliationLookup.apply(f.getId())))
                .toList();
        return new AuditExceptionReportResponse(auditId, !mapped.isEmpty(), mapped);
    }

    public AuditBatchScanResponse toBatchResponse(AuditScanService.BatchScanResult result,
                                                   java.util.function.Function<UUID, List<AuditFindingCorrection>> correctionsLookup) {
        List<AuditFindingResponse> created = result.created().stream()
                .map(f -> toResponse(f, correctionsLookup.apply(f.getId())))
                .toList();
        int verified = (int) result.created().stream().filter(f -> f.getStatus() == FindingStatus.VERIFIED).count();
        int outOfScope = (int) result.created().stream().filter(f -> f.getStatus() == FindingStatus.OUT_OF_SCOPE).count();
        AuditBatchScanResponse.Summary summary = new AuditBatchScanResponse.Summary(
                verified, outOfScope, result.duplicateAssetIds().size(), result.unrecognizedAssetIds().size());
        return new AuditBatchScanResponse(created, result.duplicateAssetIds(), result.unrecognizedAssetIds(), summary);
    }
}
