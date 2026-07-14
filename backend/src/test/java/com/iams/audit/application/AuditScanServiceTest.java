package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.domain.AssetCondition;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditAssignmentRepository;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.common.exception.ConflictException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AuditScanServiceTest {

    @Mock private AuditRepository auditRepository;
    @Mock private AuditFindingRepository findingRepository;
    @Mock private AuditExpectedAssetRepository expectedAssetRepository;
    @Mock private AuditAssignmentRepository assignmentRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private AuditScanService service;
    private UUID actorId;
    private UUID auditId;
    private Audit audit;

    @BeforeEach
    void setUp() {
        service = new AuditScanService(auditRepository, findingRepository, expectedAssetRepository,
                assignmentRepository, assetRepository, currentUserProvider);
        actorId = UUID.randomUUID();
        auditId = UUID.randomUUID();
        audit = new Audit();
        audit.setId(auditId);
        audit.setStatus(AuditStatus.IN_PROGRESS);
        org.mockito.Mockito.lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "auditor1", Set.of("AUDITOR")));
    }

    private Asset asset() {
        Asset a = new Asset();
        a.setId(UUID.randomUUID());
        a.setAssetNumber("AST-777");
        a.setName("Projector");
        return a;
    }

    @Test
    void recordScan_marksVerified_whenAssetIsInExpectedSet() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId)).thenReturn(List.of());
        Asset asset = asset();
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(findingRepository.findByAuditIdAndAssetId(auditId, asset.getId())).thenReturn(Optional.empty());
        when(expectedAssetRepository.existsByAuditIdAndAssetId(auditId, asset.getId())).thenReturn(true);
        when(findingRepository.save(any(AuditFinding.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditFinding finding = service.recordScan(auditId,
                new AuditScanCommand(asset.getId(), AssetCondition.GOOD, "Looks fine", "device-1"));

        assertThat(finding.getStatus().name()).isEqualTo("VERIFIED");
        assertThat(finding.getVerifiedByUserId()).isEqualTo(actorId);
        assertThat(finding.getDeviceId()).isEqualTo("device-1");
    }

    @Test
    void recordScan_flagsOutOfScope_whenAssetNotInExpectedSet() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId)).thenReturn(List.of());
        Asset asset = asset();
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(findingRepository.findByAuditIdAndAssetId(auditId, asset.getId())).thenReturn(Optional.empty());
        when(expectedAssetRepository.existsByAuditIdAndAssetId(auditId, asset.getId())).thenReturn(false);
        when(findingRepository.save(any(AuditFinding.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditFinding finding = service.recordScan(auditId,
                new AuditScanCommand(asset.getId(), AssetCondition.GOOD, null, null));

        assertThat(finding.getStatus().name()).isEqualTo("OUT_OF_SCOPE");
    }

    @Test
    void recordScan_rejectsDuplicateScanOfSameAsset() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId)).thenReturn(List.of());
        Asset asset = asset();
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(findingRepository.findByAuditIdAndAssetId(auditId, asset.getId()))
                .thenReturn(Optional.of(new AuditFinding()));

        assertThatThrownBy(() -> service.recordScan(auditId,
                new AuditScanCommand(asset.getId(), AssetCondition.GOOD, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordScan_rejectsWhenAuditNotInProgress() {
        audit.setStatus(AuditStatus.PENDING_APPROVAL);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> service.recordScan(auditId,
                new AuditScanCommand(UUID.randomUUID(), AssetCondition.GOOD, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordScan_rejectsUnassignedAuditor_whenAssignmentsExist() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        AuditAssignment someoneElse = new AuditAssignment();
        someoneElse.setAuditorUserId(UUID.randomUUID());
        someoneElse.setActive(true);
        when(assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId)).thenReturn(List.of(someoneElse));

        assertThatThrownBy(() -> service.recordScan(auditId,
                new AuditScanCommand(UUID.randomUUID(), AssetCondition.GOOD, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recordScan_rejectsRemarksOverLengthLimit() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId)).thenReturn(List.of());
        Asset asset = asset();
        when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(findingRepository.findByAuditIdAndAssetId(auditId, asset.getId())).thenReturn(Optional.empty());

        String tooLong = "x".repeat(1001);

        assertThatThrownBy(() -> service.recordScan(auditId,
                new AuditScanCommand(asset.getId(), AssetCondition.GOOD, tooLong, null)))
                .isInstanceOf(com.iams.common.exception.ValidationFailedException.class);
    }

    @Test
    void recordBatchScan_bucketsVerifiedDuplicateAndUnrecognized() {
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        when(assignmentRepository.findByAuditIdOrderByCreatedAtAsc(auditId)).thenReturn(List.of());

        Asset newAsset = asset();
        Asset duplicateAsset = asset();
        UUID unrecognizedId = UUID.randomUUID();

        when(assetRepository.findById(newAsset.getId())).thenReturn(Optional.of(newAsset));
        when(assetRepository.findById(duplicateAsset.getId())).thenReturn(Optional.of(duplicateAsset));
        when(assetRepository.findById(unrecognizedId)).thenReturn(Optional.empty());
        when(findingRepository.findByAuditIdAndAssetId(auditId, newAsset.getId())).thenReturn(Optional.empty());
        when(findingRepository.findByAuditIdAndAssetId(auditId, duplicateAsset.getId()))
                .thenReturn(Optional.of(new AuditFinding()));
        when(expectedAssetRepository.existsByAuditIdAndAssetId(auditId, newAsset.getId())).thenReturn(true);
        when(findingRepository.save(any(AuditFinding.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditScanService.BatchScanResult result = service.recordBatchScan(auditId, List.of(
                new AuditScanCommand(newAsset.getId(), AssetCondition.GOOD, null, null),
                new AuditScanCommand(duplicateAsset.getId(), AssetCondition.GOOD, null, null),
                new AuditScanCommand(unrecognizedId, AssetCondition.GOOD, null, null)
        ));

        assertThat(result.created()).hasSize(1);
        assertThat(result.duplicateAssetIds()).containsExactly(duplicateAsset.getId());
        assertThat(result.unrecognizedAssetIds()).containsExactly(unrecognizedId);
    }
}
