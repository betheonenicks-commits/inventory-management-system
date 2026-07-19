package com.iams.audit.application;

import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFindingReconciliationRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-AUD-18: cross-cycle audit analytics. Treats each CLOSED audit as one "cycle"
 * and produces the per-cycle metrics BO-2/BO-3 track - missing rate and completion
 * time - from system data (per BRD §1.3.1), ordered chronologically so the caller
 * can render trend lines rather than a manual tally.
 * <p>
 * Cycles are sourced through {@link AuditService#list(AuditStatus)}, so the analytics
 * are ALREADY org-scope-filtered to the caller (an Inventory Manager sees trends for
 * the audits within their scope, nothing wider - reusing the enforcement hardened for
 * no-org-node audits). The missing-rate reduction credits ONLY formal US-AUD-21
 * reconciliations (AC-AUD-18's "that reconciliation - and only that path - counts"):
 * an asset that merely reappears without a reconciliation record is never credited.
 */
@Service
public class AuditAnalyticsService {

    private static final double MINUTES_PER_DAY = 1440.0;

    private final AuditService auditService;
    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditFindingRepository findingRepository;
    private final AuditFindingReconciliationRepository reconciliationRepository;

    public AuditAnalyticsService(AuditService auditService,
                                 AuditExpectedAssetRepository expectedAssetRepository,
                                 AuditFindingRepository findingRepository,
                                 AuditFindingReconciliationRepository reconciliationRepository) {
        this.auditService = auditService;
        this.expectedAssetRepository = expectedAssetRepository;
        this.findingRepository = findingRepository;
        this.reconciliationRepository = reconciliationRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditCycleTrend> crossCycleTrends() {
        return auditService.list(AuditStatus.CLOSED).stream()
                // Chronological (oldest closed first) so trend lines read left-to-right;
                // a null approvedAt (shouldn't happen for CLOSED) sorts last, never throws.
                .sorted(Comparator.comparing(Audit::getApprovedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toCycle)
                .toList();
    }

    private AuditCycleTrend toCycle(Audit audit) {
        long expected = expectedAssetRepository.countByAuditId(audit.getId());
        long missing = findingRepository.countByAuditIdAndStatus(audit.getId(), FindingStatus.MISSING);
        long reconciled = reconciliationRepository.countByAuditId(audit.getId());
        // A reconciliation can only credit a real missing finding; never let the net go below zero.
        long netMissing = Math.max(0, missing - reconciled);
        Double completionDays = (audit.getCreatedAt() != null && audit.getApprovedAt() != null)
                ? round(Duration.between(audit.getCreatedAt(), audit.getApprovedAt()).toMinutes() / MINUTES_PER_DAY)
                : null;
        return new AuditCycleTrend(audit.getId(), audit.getName(), audit.getApprovedAt(),
                expected, missing, reconciled, netMissing,
                rate(missing, expected), rate(netMissing, expected), completionDays);
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : round((numerator * 100.0) / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * One closed audit cycle's metrics for the cross-cycle trend. {@code missingRatePct}
     * is the raw rate at close; {@code netMissingRatePct} additionally credits formal
     * US-AUD-21 reconciliations - the "reduction" BO-2/BO-3 actually care about.
     */
    public record AuditCycleTrend(UUID auditId, String name, Instant approvedAt,
                                  long expectedCount, long missingCount, long reconciledCount, long netMissingCount,
                                  double missingRatePct, double netMissingRatePct, Double completionDays) {
    }
}
