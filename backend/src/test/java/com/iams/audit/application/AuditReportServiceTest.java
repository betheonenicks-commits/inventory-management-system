package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
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
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock private AuditRepository auditRepository;
    @Mock private AuditFindingRepository findingRepository;
    @Mock private AuditExpectedAssetRepository expectedAssetRepository;
    @Mock private AuditService auditService;
    @Mock private AppUserRepository userRepository;

    private AuditReportService service;
    private UUID auditId;

    @BeforeEach
    void setUp() {
        service = new AuditReportService(auditRepository, findingRepository, expectedAssetRepository, auditService,
                userRepository);
        auditId = UUID.randomUUID();
    }

    @Test
    void exceptions_delegatesWithVerifiedStatusAndDamageConditions() {
        when(auditRepository.existsById(auditId)).thenReturn(true);
        AuditFinding missing = new AuditFinding();
        when(findingRepository.findExceptionsByAuditId(auditId, FindingStatus.VERIFIED,
                List.of(AssetCondition.MINOR_DAMAGE, AssetCondition.MAJOR_DAMAGE, AssetCondition.UNUSABLE)))
                .thenReturn(List.of(missing));

        List<AuditFinding> result = service.exceptions(auditId);

        assertThat(result).containsExactly(missing);
    }

    @Test
    void exceptions_throwsNotFound_whenAuditDoesNotExist() {
        when(auditRepository.existsById(auditId)).thenReturn(false);

        assertThatThrownBy(() -> service.exceptions(auditId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void certificate_blocksUntilAuditIsClosed() {
        Audit audit = new Audit();
        audit.setId(auditId);
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> service.certificate(auditId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void certificate_computesCountsForClosedAudit() {
        Audit audit = new Audit();
        audit.setId(auditId);
        audit.setName("Q3 Sweep");
        audit.setStatus(AuditStatus.CLOSED);
        UUID approver = UUID.randomUUID();
        audit.setApprovedBy(approver);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(expectedAssetRepository.countByAuditId(auditId)).thenReturn(5L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED)).thenReturn(3L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING)).thenReturn(1L);

        AuditFinding damaged = new AuditFinding();
        damaged.setCondition(AssetCondition.MAJOR_DAMAGE);
        AuditFinding fine = new AuditFinding();
        fine.setCondition(AssetCondition.GOOD);
        when(findingRepository.findByAuditIdWithAsset(auditId)).thenReturn(List.of(damaged, fine));
        AppUser approverUser = new AppUser();
        approverUser.setDisplayName("Dana Head");
        when(userRepository.findById(approver)).thenReturn(Optional.of(approverUser));

        AuditReportService.AuditCertificate cert = service.certificate(auditId);

        assertThat(cert.expectedCount()).isEqualTo(5);
        assertThat(cert.verifiedCount()).isEqualTo(3);
        assertThat(cert.missingCount()).isEqualTo(1);
        assertThat(cert.damagedCount()).isEqualTo(1);
        assertThat(cert.approvedBy()).isEqualTo(approver);
        assertThat(cert.approverName()).isEqualTo("Dana Head");
    }

    @Test
    void recentlyClosed_ordersByApprovalNewestFirst_andHonorsLimit() {
        Audit older = closedAudit("Older", java.time.Instant.parse("2026-01-01T00:00:00Z"));
        Audit newer = closedAudit("Newer", java.time.Instant.parse("2026-06-01T00:00:00Z"));
        Audit noStamp = closedAudit("NoStamp", null);
        // Returned in a deliberately unsorted order; the service must sort them.
        when(auditService.list(AuditStatus.CLOSED)).thenReturn(List.of(older, newer, noStamp));
        lenient().when(auditService.progress(any())).thenReturn(new AuditService.AuditProgress(1, 1, 0, 0, 0));
        lenient().when(findingRepository.findExceptionsByAuditId(any(), any(), anyList())).thenReturn(List.of());

        List<AuditReportService.AuditDashboardItem> recent = service.recentlyClosed(2);

        // Newest approval first; the null-approvedAt audit would sort last but is cut by the limit of 2.
        assertThat(recent).extracting(AuditReportService.AuditDashboardItem::name).containsExactly("Newer", "Older");
    }

    private Audit closedAudit(String name, java.time.Instant approvedAt) {
        Audit audit = new Audit();
        audit.setId(UUID.randomUUID());
        audit.setName(name);
        audit.setStatus(AuditStatus.CLOSED);
        audit.setApprovedAt(approvedAt);
        return audit;
    }

    @Test
    void certificate_staysRetrievable_whenApproverAccountWasSinceDeleted() {
        Audit audit = new Audit();
        audit.setId(auditId);
        audit.setName("Old Audit");
        audit.setStatus(AuditStatus.CLOSED);
        UUID goneApprover = UUID.randomUUID();
        audit.setApprovedBy(goneApprover);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(userRepository.findById(goneApprover)).thenReturn(Optional.empty());
        when(findingRepository.findByAuditIdWithAsset(auditId)).thenReturn(List.of());

        AuditReportService.AuditCertificate cert = service.certificate(auditId);

        assertThat(cert.approvedBy()).isEqualTo(goneApprover);
        assertThat(cert.approverName()).isNull();
    }
}
