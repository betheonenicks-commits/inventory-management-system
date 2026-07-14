package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private AuditReportService service;
    private UUID auditId;

    @BeforeEach
    void setUp() {
        service = new AuditReportService(auditRepository, findingRepository, expectedAssetRepository, auditService);
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

        AuditReportService.AuditCertificate cert = service.certificate(auditId);

        assertThat(cert.expectedCount()).isEqualTo(5);
        assertThat(cert.verifiedCount()).isEqualTo(3);
        assertThat(cert.missingCount()).isEqualTo(1);
        assertThat(cert.damagedCount()).isEqualTo(1);
        assertThat(cert.approvedBy()).isEqualTo(approver);
    }
}
