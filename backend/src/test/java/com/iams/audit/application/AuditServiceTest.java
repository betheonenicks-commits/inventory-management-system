package com.iams.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditAssignmentRepository;
import com.iams.audit.domain.AuditExpectedAsset;
import com.iams.audit.domain.AuditExpectedAssetRepository;
import com.iams.audit.domain.AuditFindingRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.audit.domain.AuditSubScopeCount;
import com.iams.audit.domain.AuditSubScopeStatusCount;
import com.iams.audit.domain.AuditType;
import com.iams.audit.domain.FindingStatus;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditRepository auditRepository;
    @Mock private AuditExpectedAssetRepository expectedAssetRepository;
    @Mock private AuditAssignmentRepository assignmentRepository;
    @Mock private AuditFindingRepository findingRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;

    private AuditService service;
    private UUID actorId;
    private UUID approverId;

    @BeforeEach
    void setUp() {
        service = new AuditService(auditRepository, expectedAssetRepository, assignmentRepository, findingRepository,
                assetRepository, orgNodeRepository, categoryRepository, appUserRepository, currentUserProvider, scopeGuard);
        actorId = UUID.randomUUID();
        approverId = UUID.randomUUID();
        org.mockito.Mockito.lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(actorId, "auditor1", Set.of("AUDITOR")));
    }

    private Asset asset(String number) {
        Asset a = new Asset();
        a.setId(UUID.randomUUID());
        a.setAssetNumber(number);
        a.setName("Test Asset " + number);
        return a;
    }

    @Test
    void create_rejectsEmptyScope() {
        AuditCreateCommand command = new AuditCreateCommand("Q3 Audit", AuditType.SPOT_CHECK, null, null, null, approverId, null);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("scoping criterion");
    }

    @Test
    void create_resolvesAndFreezesExpectedAssetsFromOrgNodeScope() {
        UUID orgNodeId = UUID.randomUUID();
        OrgNode orgNode = new OrgNode();
        orgNode.setId(orgNodeId);
        orgNode.setPath("/campus/building-b/");
        when(orgNodeRepository.findById(orgNodeId)).thenReturn(Optional.of(orgNode));
        when(appUserRepository.existsById(approverId)).thenReturn(true);

        Audit saved = new Audit();
        saved.setId(UUID.randomUUID());
        when(auditRepository.save(any(Audit.class))).thenReturn(saved);
        when(auditRepository.findByIdWithAssociations(saved.getId())).thenReturn(Optional.of(saved));

        Asset a1 = asset("AST-001");
        Asset a2 = asset("AST-002");
        when(assetRepository.search(isNull(), isNull(), isNull(), isNull(), eq("/campus/building-b/"), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(a1, a2)));

        Audit result = service.create(new AuditCreateCommand("Building B Sweep", AuditType.BULK, orgNodeId, null, null, approverId, null));

        assertThat(result).isSameAs(saved);
        verify(expectedAssetRepository, org.mockito.Mockito.times(2)).save(any(AuditExpectedAsset.class));
    }

    @Test
    void create_withExplicitAssetList_usesThoseAssetsExactly() {
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        Asset a1 = asset("AST-010");
        List<UUID> ids = List.of(a1.getId());
        when(assetRepository.findAllById(ids)).thenReturn(List.of(a1));

        Audit saved = new Audit();
        saved.setId(UUID.randomUUID());
        when(auditRepository.save(any(Audit.class))).thenReturn(saved);
        when(auditRepository.findByIdWithAssociations(saved.getId())).thenReturn(Optional.of(saved));

        service.create(new AuditCreateCommand("Spot Check", AuditType.SPOT_CHECK, null, null, ids, approverId, null));

        verify(expectedAssetRepository, org.mockito.Mockito.times(1)).save(any(AuditExpectedAsset.class));
    }

    @Test
    void create_rejectsUnknownAssetIdInExplicitList() {
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        UUID unknownId = UUID.randomUUID();
        when(assetRepository.findAllById(List.of(unknownId))).thenReturn(List.of());

        Audit saved = new Audit();
        saved.setId(UUID.randomUUID());
        when(auditRepository.save(any(Audit.class))).thenReturn(saved);

        assertThatThrownBy(() -> service.create(
                new AuditCreateCommand("Spot Check", AuditType.SPOT_CHECK, null, null, List.of(unknownId), approverId, null)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownNominalApprover() {
        when(appUserRepository.existsById(approverId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new AuditCreateCommand("Q3", AuditType.ANNUAL, null, null, List.of(UUID.randomUUID()), approverId, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_throwsAccessDenied_whenOutsideCallerScope() {
        UUID auditId = UUID.randomUUID();
        UUID orgNodeId = UUID.randomUUID();
        OrgNode orgNode = new OrgNode();
        orgNode.setId(orgNodeId);
        orgNode.setPath("/campus/building-c/");

        Audit audit = new Audit();
        audit.setId(auditId);
        audit.setScopeOrgNode(orgNode);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));
        org.mockito.Mockito.doThrow(new AccessDeniedException("out of scope"))
                .when(scopeGuard).requireWithinScope(orgNodeId, "audit", auditId);

        assertThatThrownBy(() -> service.get(auditId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignAuditor_createsActiveAssignmentSnapshottingUsername() {
        UUID auditId = UUID.randomUUID();
        Audit audit = new Audit();
        audit.setId(auditId);
        when(auditRepository.findByIdWithAssociations(auditId)).thenReturn(Optional.of(audit));

        UUID auditorUserId = UUID.randomUUID();
        AppUser auditor = new AppUser();
        auditor.setId(auditorUserId);
        auditor.setUsername("field.auditor");
        when(appUserRepository.findById(auditorUserId)).thenReturn(Optional.of(auditor));
        when(assignmentRepository.save(any(AuditAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditAssignment result = service.assignAuditor(auditId, auditorUserId, "Floor 2");

        assertThat(result.getAuditorUsername()).isEqualTo("field.auditor");
        assertThat(result.getSubScope()).isEqualTo("Floor 2");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void unassignAuditor_endsAssignmentButKeepsRow() {
        UUID auditId = UUID.randomUUID();
        Audit audit = new Audit();
        audit.setId(auditId);
        UUID assignmentId = UUID.randomUUID();
        AuditAssignment assignment = new AuditAssignment();
        assignment.setId(assignmentId);
        assignment.setAudit(audit);
        assignment.setActive(true);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.saveAndFlush(assignment)).thenReturn(assignment);

        AuditAssignment result = service.unassignAuditor(auditId, assignmentId);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getUnassignedAt()).isNotNull();
    }

    @Test
    void progress_countsEachFindingStatusSeparately() {
        UUID auditId = UUID.randomUUID();
        when(auditRepository.existsById(auditId)).thenReturn(true);
        when(expectedAssetRepository.countByAuditId(auditId)).thenReturn(10L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED)).thenReturn(6L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING)).thenReturn(2L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.OUT_OF_SCOPE)).thenReturn(1L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.SCOPE_CHANGED)).thenReturn(1L);

        AuditService.AuditProgress progress = service.progress(auditId);

        assertThat(progress.expectedCount()).isEqualTo(10);
        assertThat(progress.verifiedCount()).isEqualTo(6);
        assertThat(progress.missingCount()).isEqualTo(2);
    }

    @Test
    void progressDetail_breaksDownBySubScope_andReconcilesWithFlatTotals() {
        UUID auditId = UUID.randomUUID();
        UUID buildingA = UUID.randomUUID();
        UUID buildingB = UUID.randomUUID();
        when(auditRepository.existsById(auditId)).thenReturn(true);
        // Flat totals: 10 expected, 6 verified, 2 missing, 1 out-of-scope, 1 scope-changed.
        when(expectedAssetRepository.countByAuditId(auditId)).thenReturn(10L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED)).thenReturn(6L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING)).thenReturn(2L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.OUT_OF_SCOPE)).thenReturn(1L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.SCOPE_CHANGED)).thenReturn(1L);
        // Sub-scope aggregates: Building A holds 7 expected, Building B holds 3.
        when(expectedAssetRepository.countExpectedByOrgNode(auditId)).thenReturn(List.of(
                new AuditSubScopeCount(buildingA, "Building A", "BA", 7L),
                new AuditSubScopeCount(buildingB, "Building B", "BB", 3L)));
        when(findingRepository.countFindingsByOrgNodeAndStatus(auditId)).thenReturn(List.of(
                new AuditSubScopeStatusCount(buildingA, "Building A", "BA", FindingStatus.VERIFIED, 5L),
                new AuditSubScopeStatusCount(buildingA, "Building A", "BA", FindingStatus.MISSING, 1L),
                new AuditSubScopeStatusCount(buildingA, "Building A", "BA", FindingStatus.SCOPE_CHANGED, 1L),
                new AuditSubScopeStatusCount(buildingB, "Building B", "BB", FindingStatus.VERIFIED, 1L),
                new AuditSubScopeStatusCount(buildingB, "Building B", "BB", FindingStatus.MISSING, 1L),
                new AuditSubScopeStatusCount(buildingB, "Building B", "BB", FindingStatus.OUT_OF_SCOPE, 1L)));

        AuditService.AuditProgressDetail detail = service.progressDetail(auditId);

        // Flat totals unchanged.
        assertThat(detail.totals().expectedCount()).isEqualTo(10);
        assertThat(detail.totals().verifiedCount()).isEqualTo(6);
        // Two sub-scopes, sorted by name (Building A then Building B).
        assertThat(detail.subScopes()).hasSize(2);
        AuditService.SubScopeProgress a = detail.subScopes().get(0);
        AuditService.SubScopeProgress b = detail.subScopes().get(1);
        assertThat(a.orgNodeName()).isEqualTo("Building A");
        assertThat(a.expectedCount()).isEqualTo(7);
        assertThat(a.verifiedCount()).isEqualTo(5);
        assertThat(a.missingCount()).isEqualTo(1);
        assertThat(a.scopeChangedCount()).isEqualTo(1);
        assertThat(a.outOfScopeCount()).isZero();
        assertThat(b.orgNodeName()).isEqualTo("Building B");
        assertThat(b.expectedCount()).isEqualTo(3);
        assertThat(b.outOfScopeCount()).isEqualTo(1);
        // The invariant this design guarantees: every column sums back to the flat total.
        assertThat(a.expectedCount() + b.expectedCount()).isEqualTo(detail.totals().expectedCount());
        assertThat(a.verifiedCount() + b.verifiedCount()).isEqualTo(detail.totals().verifiedCount());
        assertThat(a.missingCount() + b.missingCount()).isEqualTo(detail.totals().missingCount());
        assertThat(a.outOfScopeCount() + b.outOfScopeCount()).isEqualTo(detail.totals().outOfScopeCount());
        assertThat(a.scopeChangedCount() + b.scopeChangedCount()).isEqualTo(detail.totals().scopeChangedCount());
    }

    @Test
    void progressDetail_surfacesAnOutOfScopeFindingAtALocationWithNoExpectedAsset() {
        UUID auditId = UUID.randomUUID();
        UUID expectedLoc = UUID.randomUUID();
        UUID strayLoc = UUID.randomUUID();
        when(auditRepository.existsById(auditId)).thenReturn(true);
        when(expectedAssetRepository.countByAuditId(auditId)).thenReturn(2L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.VERIFIED)).thenReturn(2L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.MISSING)).thenReturn(0L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.OUT_OF_SCOPE)).thenReturn(1L);
        when(findingRepository.countByAuditIdAndStatus(auditId, FindingStatus.SCOPE_CHANGED)).thenReturn(0L);
        when(expectedAssetRepository.countExpectedByOrgNode(auditId)).thenReturn(List.of(
                new AuditSubScopeCount(expectedLoc, "Room 101", "R101", 2L)));
        // The out-of-scope find is at a location that has NO expected asset - it must still appear.
        when(findingRepository.countFindingsByOrgNodeAndStatus(auditId)).thenReturn(List.of(
                new AuditSubScopeStatusCount(expectedLoc, "Room 101", "R101", FindingStatus.VERIFIED, 2L),
                new AuditSubScopeStatusCount(strayLoc, "Corridor", "CORR", FindingStatus.OUT_OF_SCOPE, 1L)));

        AuditService.AuditProgressDetail detail = service.progressDetail(auditId);

        assertThat(detail.subScopes()).hasSize(2);
        AuditService.SubScopeProgress stray = detail.subScopes().stream()
                .filter(s -> s.orgNodeName().equals("Corridor")).findFirst().orElseThrow();
        assertThat(stray.expectedCount()).isZero();
        assertThat(stray.outOfScopeCount()).isEqualTo(1);
        // Out-of-scope column still reconciles once the union brings the stray location in.
        long outOfScopeSum = detail.subScopes().stream().mapToLong(AuditService.SubScopeProgress::outOfScopeCount).sum();
        assertThat(outOfScopeSum).isEqualTo(detail.totals().outOfScopeCount());
    }

    @Test
    void progressDetail_emptyAuditHasNoSubScopes() {
        UUID auditId = UUID.randomUUID();
        when(auditRepository.existsById(auditId)).thenReturn(true);
        when(expectedAssetRepository.countByAuditId(auditId)).thenReturn(0L);
        when(findingRepository.countByAuditIdAndStatus(any(), any())).thenReturn(0L);
        when(expectedAssetRepository.countExpectedByOrgNode(auditId)).thenReturn(List.of());
        when(findingRepository.countFindingsByOrgNodeAndStatus(auditId)).thenReturn(List.of());

        AuditService.AuditProgressDetail detail = service.progressDetail(auditId);

        assertThat(detail.totals().expectedCount()).isZero();
        assertThat(detail.subScopes()).isEmpty();
    }

    @Test
    void progressDetail_unknownAudit404s() {
        UUID auditId = UUID.randomUUID();
        when(auditRepository.existsById(auditId)).thenReturn(false);

        assertThatThrownBy(() -> service.progressDetail(auditId)).isInstanceOf(NotFoundException.class);
    }

}
