package com.iams.audit.application;

import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-AUD-15/16/17: the exception report, completion certificate, and the active-audit dashboard summary. */
@Service
public class AuditReportService {

    private static final List<AssetCondition> DAMAGE_CONDITIONS =
            List.of(AssetCondition.MINOR_DAMAGE, AssetCondition.MAJOR_DAMAGE, AssetCondition.UNUSABLE);

    private final AuditRepository auditRepository;
    private final AuditFindingRepository findingRepository;
    private final AuditExpectedAssetRepository expectedAssetRepository;
    private final AuditService auditService;

    public AuditReportService(AuditRepository auditRepository, AuditFindingRepository findingRepository,
                               AuditExpectedAssetRepository expectedAssetRepository, AuditService auditService) {
        this.auditRepository = auditRepository;
        this.findingRepository = findingRepository;
        this.expectedAssetRepository = expectedAssetRepository;
        this.auditService = auditService;
    }

    /** US-AUD-16: anything not a clean Verified find - Missing, Out of Scope, Scope Changed, or Verified-but-damaged. */
    @Transactional(readOnly = true)
    public List<AuditFinding> exceptions(UUID auditId) {
        if (!auditRepository.existsById(auditId)) {
            throw NotFoundException.of("Audit", auditId);
        }
        return findingRepository.findExceptionsByAuditId(auditId, FindingStatus.VERIFIED, DAMAGE_CONDITIONS);
    }

    /** US-AUD-15: only available once an audit is approved and closed - AC-AUD-15-X blocks closure itself while scope-change dispositions are open, so a certificate existing at all implies that gate already passed. */
    @Transactional(readOnly = true)
    public AuditCertificate certificate(UUID auditId) {
        Audit audit = auditRepository.findByIdWithAssociations(auditId).orElseThrow(() -> NotFoundException.of("Audit", auditId));
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
        return new AuditCertificate(audit.getId(), audit.getName(), expected, verified, missing, damaged,
                audit.getApprovedBy(), audit.getApprovedAt());
    }

    /** US-AUD-17: active audits in the caller's scope, with live progress and exception counts - no separate "recent/closed" section built this session. */
    @Transactional(readOnly = true)
    public List<AuditDashboardItem> dashboard() {
        return auditService.list(null).stream()
                .filter(a -> a.getStatus() != AuditStatus.CLOSED)
                .map(a -> new AuditDashboardItem(a.getId(), a.getName(), a.getStatus(), auditService.progress(a.getId()),
                        findingRepository.findExceptionsByAuditId(a.getId(), FindingStatus.VERIFIED, DAMAGE_CONDITIONS).size()))
                .toList();
    }

    public record AuditCertificate(UUID auditId, String auditName, long expectedCount, long verifiedCount,
                                    long missingCount, long damagedCount, UUID approvedBy, Instant approvedAt) {
    }

    public record AuditDashboardItem(UUID auditId, String name, AuditStatus status, AuditService.AuditProgress progress,
                                      long exceptionCount) {
    }
}
