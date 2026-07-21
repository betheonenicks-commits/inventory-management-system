package com.iams.audit.application;

import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-AUD-15/16/17: the exception report, completion certificate, and the active-audit dashboard summary. */
@Service
public class AuditReportService {

    private static final List<AssetCondition> DAMAGE_CONDITIONS =
            List.of(AssetCondition.MINOR_DAMAGE, AssetCondition.MAJOR_DAMAGE, AssetCondition.UNUSABLE);

    private final AuditFindingRepository findingRepository;
    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditService auditService;
    private final AppUserRepository userRepository;

    public AuditReportService(AuditFindingRepository findingRepository,
                               AuditExpectedAssetRepository expectedAssetRepository, AuditService auditService,
                               AppUserRepository userRepository) {
        this.findingRepository = findingRepository;
        this.expectedAssetRepository = expectedAssetRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /** US-AUD-16: anything not a clean Verified find - Missing, Out of Scope, Scope Changed, or Verified-but-damaged. */
    @Transactional(readOnly = true)
    public List<AuditFinding> exceptions(UUID auditId) {
        auditService.get(auditId); // US-AUD-16: throws NotFound, and enforces org-scope (was existsById-only)
        return findingRepository.findExceptionsByAuditId(auditId, FindingStatus.VERIFIED, DAMAGE_CONDITIONS);
    }

    /** US-AUD-15: only available once an audit is approved and closed - AC-AUD-15-X blocks closure itself while scope-change dispositions are open, so a certificate existing at all implies that gate already passed. */
    @Transactional(readOnly = true)
    public AuditCertificate certificate(UUID auditId) {
        Audit audit = auditService.get(auditId); // enforces org-scope, not just a direct repository load
        if (audit.getStatus() != AuditStatus.CLOSED) {
            throw new ConflictException("AUDIT_NOT_CLOSED",
                    "A completion certificate is only available once an audit is approved and closed");
        }
        long expected = expectedAssetRepository.countByAuditId(auditId);
        long verified = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED);
        long missing = findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING);
        long damaged = findingRepository.findByAuditIdWithAsset(auditId).stream()
                .filter(f -> f.getCondition() != null && DAMAGE_CONDITIONS.contains(f.getCondition()))
                .count();
        // A formal certificate names its approver; a UUID is an implementation
        // detail. Resolved leniently - a since-deleted approver account must
        // not make a historical certificate unretrievable.
        String approverName = audit.getApprovedBy() == null ? null
                : userRepository.findById(audit.getApprovedBy()).map(AppUser::getDisplayName).orElse(null);
        return new AuditCertificate(audit.getId(), audit.getName(), expected, verified, missing, damaged,
                audit.getApprovedBy(), approverName, audit.getApprovedAt());
    }

    /** US-AUD-17: active (non-closed) audits in the caller's scope, with live progress and exception counts. */
    @Transactional(readOnly = true)
    public List<AuditDashboardItem> dashboard() {
        return auditService.list(null).stream()
                .filter(a -> a.getStatus() != AuditStatus.CLOSED)
                .map(this::toDashboardItem)
                .toList();
    }

    /**
     * US-AUD-17: the "recent" half of the dashboard - most-recently-approved
     * closed audits in the caller's scope, newest first, capped at {@code limit}.
     * A closed audit with a null approvedAt (data from before that field was
     * populated) sorts last rather than throwing.
     */
    @Transactional(readOnly = true)
    public List<AuditDashboardItem> recentlyClosed(int limit) {
        return auditService.list(AuditStatus.CLOSED).stream()
                .sorted(Comparator.comparing(Audit::getApprovedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(0, limit))
                .map(this::toDashboardItem)
                .toList();
    }

    private AuditDashboardItem toDashboardItem(Audit a) {
        return new AuditDashboardItem(a.getId(), a.getName(), a.getStatus(), auditService.progress(a.getId()),
                findingRepository.findExceptionsByAuditId(a.getId(), FindingStatus.VERIFIED, DAMAGE_CONDITIONS).size());
    }

    public record AuditCertificate(UUID auditId, String auditName, long expectedCount, long verifiedCount,
                                    long missingCount, long damagedCount, UUID approvedBy, String approverName,
                                    Instant approvedAt) {
    }

    public record AuditDashboardItem(UUID auditId, String name, AuditStatus status, AuditService.AuditProgress progress,
                                      long exceptionCount) {
    }
}
