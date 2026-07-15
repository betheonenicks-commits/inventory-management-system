package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditStatus;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.dashboard.application.DashboardService;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.Person;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.PersonRepository;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private AssetRepository assetRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private PersonRepository personRepository;
    @Mock private SecurityEventLogRepository securityEventLogRepository;
    @Mock private ReportQueries reportQueries;
    @Mock private DashboardService dashboardService;
    @Mock private OrgScopeGuard scopeGuard;
    @Mock private com.iams.audit.domain.AuditRepository auditRepository;
    @Mock private com.iams.audit.application.AuditReportService auditReportService;
    @Mock private com.iams.asset.application.DepreciationService depreciationService;
    @Mock private com.iams.maintenance.domain.RepairEventRepository repairEventRepository;
    @Mock private com.iams.maintenance.domain.MaintenanceEventRepository maintenanceEventRepository;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(assetRepository, orgNodeRepository, personRepository,
                securityEventLogRepository, reportQueries, dashboardService, scopeGuard,
                auditRepository, auditReportService, depreciationService, repairEventRepository,
                maintenanceEventRepository);
    }

    @Test
    void assetRegister_walksEveryPageIntoOneReport() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        Asset a1 = asset("AST-1", "Laptop");
        Asset a2 = asset("AST-2", "Projector");
        when(assetRepository.search(eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(a1), PageRequest.of(0, 500), 501))
                .thenReturn(new PageImpl<>(List.of(a2), PageRequest.of(1, 500), 501));

        TabularReport report = service.assetRegister(null, null, null);

        assertThat(report.rows()).hasSize(2);
        assertThat(report.rows().get(0).get(0)).isEqualTo("AST-1");
        assertThat(report.rows().get(1).get(0)).isEqualTo("AST-2");
        assertThat(report.columns()).hasSameSizeAs(report.rows().get(0));
    }

    @Test
    void assetRegister_requestedNodeOutsideCallerScopeIsRefusedNotEmptied() {
        UUID nodeId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new AccessDeniedException("outside scope"))
                .when(scopeGuard).requireWithinScope(eq(nodeId), any(), any());

        assertThatThrownBy(() -> service.assetRegister(nodeId, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assetRegister_narrowsToRequestedNodePath() {
        UUID nodeId = UUID.randomUUID();
        OrgNode node = new OrgNode();
        node.setId(nodeId);
        node.setName("Building A");
        node.setPath("/root/bldg-a/");
        when(orgNodeRepository.findById(nodeId)).thenReturn(Optional.of(node));
        when(assetRepository.search(any(), any(), any(), any(), eq("/root/bldg-a/"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        TabularReport report = service.assetRegister(nodeId, null, null);

        assertThat(report.title()).contains("Building A");
        verify(assetRepository).search(eq(null), eq(null), eq(null), eq(null), eq("/root/bldg-a/"), eq(null), eq(null), any());
    }

    @Test
    void employeeAssets_unknownPersonIs404() {
        UUID personId = UUID.randomUUID();
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.employeeAssets(personId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void employeeAssets_emptyAssignmentsIsAnExplicitEmptyReportNotAnError() {
        UUID personId = UUID.randomUUID();
        Person person = new Person();
        person.setFullName("J Smith");
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of());
        passThroughScopeFilter();
        when(reportQueries.assignmentEvents(List.of())).thenReturn(List.of());

        TabularReport report = service.employeeAssets(personId);

        assertThat(report.rows()).isEmpty();
        assertThat(report.title()).contains("J Smith");
    }

    @Test
    void employeeAssets_latestAssignmentEventWinsPerAsset() {
        UUID personId = UUID.randomUUID();
        Person person = new Person();
        person.setFullName("J Smith");
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        Asset asset = asset("AST-9", "Camera");
        when(assetRepository.findByAssignedToPersonId(personId)).thenReturn(List.of(asset));
        passThroughScopeFilter();
        Instant newer = Instant.parse("2026-07-01T10:00:00Z");
        Instant older = Instant.parse("2026-01-01T10:00:00Z");
        when(reportQueries.assignmentEvents(any())).thenReturn(List.of(
                assignmentEvent(asset, newer), assignmentEvent(asset, older)));

        TabularReport report = service.employeeAssets(personId);

        assertThat(report.rows().get(0).get(5)).isEqualTo(newer.toString());
    }

    @Test
    void assetMovements_rejectsInvertedRange() {
        assertThatThrownBy(() -> service.assetMovements(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void assetMovements_endDateIsInclusive() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        when(reportQueries.movements(any(), any(), any())).thenReturn(List.of());

        service.assetMovements(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10));

        // to-date + 1 day at UTC midnight = exclusive upper bound covering all of the 10th.
        verify(reportQueries).movements(
                eq(Instant.parse("2026-07-01T00:00:00Z")),
                eq(Instant.parse("2026-07-11T00:00:00Z")),
                eq(null));
    }

    @Test
    void loss_rejectsInvertedRangeButAcceptsOpenEnded() {
        assertThatThrownBy(() -> service.loss(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(ValidationFailedException.class);
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        when(reportQueries.lossFindings(any(), any(), any(), any())).thenReturn(List.of());
        assertThat(service.loss(null, null).rows()).isEmpty();
    }

    @Test
    void vendorPurchases_groupsByVendorWithSubtotalsAndGrandTotal() {
        var vendorA = vendor("Acme");
        var po1 = purchaseOrder("PO-2026-000001", vendorA, null);
        var po2 = purchaseOrder("PO-2026-000002", null, "Legacy Freetext Vendor");
        when(reportQueries.purchaseOrderLines(any(), any())).thenReturn(List.of(
                line(po1, "Desks", 10, new java.math.BigDecimal("100.00")),
                line(po1, "Chairs", 5, new java.math.BigDecimal("40.00")),
                line(po2, "Cables", 3, new java.math.BigDecimal("10.00"))));

        TabularReport report = service.vendorPurchases(null, null);

        // 3 detail + 2 vendor subtotals + 1 grand total
        assertThat(report.rows()).hasSize(6);
        assertThat(report.rows().get(2).get(0)).isEqualTo("Acme — subtotal");
        assertThat(report.rows().get(2).get(8)).isEqualTo("1200.00"); // 10*100 + 5*40
        assertThat(report.rows().get(4).get(0)).isEqualTo("Legacy Freetext Vendor — subtotal");
        assertThat(report.rows().get(5).get(0)).isEqualTo("TOTAL");
        assertThat(report.rows().get(5).get(8)).isEqualTo("1230.00");
    }

    @Test
    void auditCompliance_ratesComputeAndOnTimeDenominatorOnlyCountsAuditsWithDeadlines() {
        Audit closedOnTime = complianceAudit("On time", AuditStatus.CLOSED,
                LocalDate.of(2026, 7, 10), Instant.parse("2026-07-09T10:00:00Z"));
        Audit closedLate = complianceAudit("Late", AuditStatus.CLOSED,
                LocalDate.of(2026, 7, 1), Instant.parse("2026-07-05T10:00:00Z"));
        Audit openNoDeadline = complianceAudit("Open", AuditStatus.IN_PROGRESS, null, null);
        when(auditRepository.findAllWithAssociationsOrderByCreatedAtDesc())
                .thenReturn(List.of(closedOnTime, closedLate, openNoDeadline));
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        when(auditReportService.exceptions(closedOnTime.getId())).thenReturn(List.of());
        when(auditReportService.exceptions(closedLate.getId()))
                .thenReturn(List.of(org.mockito.Mockito.mock(com.iams.audit.domain.AuditFinding.class)));

        TabularReport report = service.auditCompliance(null, null);

        assertThat(report.rows().get(0).get(1)).startsWith("67%").contains("2/3 closed");
        assertThat(report.rows().get(1).get(1)).startsWith("50%").contains("1/2 closed audits with exceptions");
        assertThat(report.rows().get(2).get(1)).startsWith("50%").contains("1/2 audits with a scheduled date");
        // 3 summary rows + 3 audit rows; the open audit is n/a, never "complete".
        assertThat(report.rows()).hasSize(6);
        assertThat(report.rows().get(5).get(1)).isEqualTo("IN_PROGRESS");
        assertThat(report.rows().get(5).get(5)).isEqualTo("n/a (open)");
    }

    @Test
    void depreciation_flagsNotDepreciatedAndDegradesUnsupportedPerRowNotWholeReport() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        Asset computed = asset("AST-1", "Laptop");
        Asset unconfigured = asset("AST-2", "Whiteboard");
        Asset unsupported = asset("AST-3", "Vehicle");
        when(assetRepository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(computed, unconfigured, unsupported)));
        LocalDate asOf = LocalDate.of(2026, 7, 15);
        when(depreciationService.compute(computed.getId(), asOf)).thenReturn(new com.iams.asset.application.DepreciationResult(
                com.iams.asset.application.DepreciationResult.Status.COMPUTED,
                com.iams.asset.domain.DepreciationMethod.STRAIGHT_LINE, 36,
                new java.math.BigDecimal("100.00"), new java.math.BigDecimal("25.00"),
                new java.math.BigDecimal("300.00"), new java.math.BigDecimal("700.00"), asOf));
        when(depreciationService.compute(unconfigured.getId(), asOf))
                .thenReturn(com.iams.asset.application.DepreciationResult.notDepreciated(asOf));
        when(depreciationService.compute(unsupported.getId(), asOf))
                .thenThrow(ValidationFailedException.singleField("method", "DECLINING_BALANCE is not yet supported"));

        TabularReport report = service.depreciation(asOf);

        assertThat(report.rows()).hasSize(3);
        assertThat(report.rows().get(0).get(3)).isEqualTo("COMPUTED");
        assertThat(report.rows().get(0).get(7)).isEqualTo("700.00");
        assertThat(report.rows().get(1).get(3)).isEqualTo("NOT_DEPRECIATED");
        assertThat(report.rows().get(2).get(3)).isEqualTo("UNSUPPORTED");
    }

    @Test
    void maintenanceHistory_mergesRepairsAndEventsNewestFirstWithDowntimeOnlyForClosedRepairs() {
        Asset asset = asset("AST-7", "Generator");
        com.iams.maintenance.domain.RepairEvent closedRepair = new com.iams.maintenance.domain.RepairEvent();
        closedRepair.setAsset(asset);
        closedRepair.setCreatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        closedRepair.setActualReturnDate(LocalDate.of(2026, 7, 3));
        closedRepair.setActualCost(new java.math.BigDecimal("120.00"));
        closedRepair.setReason("Fan failure");
        com.iams.maintenance.domain.MaintenanceEvent preventive = new com.iams.maintenance.domain.MaintenanceEvent();
        preventive.setAsset(asset);
        preventive.setMaintenanceType(com.iams.maintenance.domain.MaintenanceType.PREVENTIVE);
        preventive.setPerformedAt(Instant.parse("2026-07-10T00:00:00Z"));
        when(repairEventRepository.findAllWithAssetOrderByCreatedAtDesc()).thenReturn(List.of(closedRepair));
        when(maintenanceEventRepository.findAllWithAssociationsOrderByPerformedAtDesc()).thenReturn(List.of(preventive));
        passThroughScopeFilter();

        TabularReport report = service.maintenanceHistory(null);

        assertThat(report.rows()).hasSize(2);
        assertThat(report.rows().get(0).get(2)).isEqualTo("PREVENTIVE"); // newest first
        assertThat(report.rows().get(1).get(2)).isEqualTo("REPAIR");
        assertThat(report.rows().get(1).get(5)).isEqualTo("3"); // Jul 1 -> end of Jul 3 = 3 days
        assertThat(report.rows().get(0).get(5)).isEmpty();
    }

    @Test
    void maintenanceHistory_unknownAssetFilterIs404() {
        UUID unknown = UUID.randomUUID();
        when(assetRepository.existsById(unknown)).thenReturn(false);
        assertThatThrownBy(() -> service.maintenanceHistory(unknown)).isInstanceOf(NotFoundException.class);
    }

    private static com.iams.inventory.domain.Vendor vendor(String name) {
        com.iams.inventory.domain.Vendor vendor = new com.iams.inventory.domain.Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName(name);
        return vendor;
    }

    private static com.iams.procurement.domain.PurchaseOrder purchaseOrder(String number,
            com.iams.inventory.domain.Vendor vendor, String freeTextVendor) {
        com.iams.procurement.domain.PurchaseOrder order = new com.iams.procurement.domain.PurchaseOrder();
        order.setId(UUID.randomUUID());
        order.setPoNumber(number);
        order.setVendor(vendor);
        order.setVendorName(vendor != null ? vendor.getName() : freeTextVendor);
        return order;
    }

    private static com.iams.procurement.domain.PurchaseOrderLine line(
            com.iams.procurement.domain.PurchaseOrder order, String description, int qty, java.math.BigDecimal unitCost) {
        com.iams.procurement.domain.PurchaseOrderLine line = new com.iams.procurement.domain.PurchaseOrderLine();
        line.setPurchaseOrder(order);
        line.setDescription(description);
        line.setQuantityOrdered(qty);
        line.setUnitCost(unitCost);
        return line;
    }

    private static Audit complianceAudit(String name, AuditStatus status, LocalDate scheduled, Instant submittedAt) {
        Audit audit = new Audit();
        audit.setId(UUID.randomUUID());
        audit.setName(name);
        audit.setStatus(status);
        audit.setScheduledDate(scheduled);
        audit.setSubmittedAt(submittedAt);
        audit.setCreatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        return audit;
    }

    @SuppressWarnings("unchecked")
    private void passThroughScopeFilter() {
        lenient().when(scopeGuard.filterToScope(any(List.class), any(Function.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static Asset asset(String number, String name) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber(number);
        asset.setName(name);
        AssetCategory category = new AssetCategory();
        category.setName("IT");
        asset.setCategory(category);
        AssetStatusDef status = new AssetStatusDef();
        status.setLabel("In Use");
        asset.setStatus(status);
        OrgNode node = new OrgNode();
        node.setId(UUID.randomUUID());
        node.setName("HQ");
        asset.setOrgNode(node);
        return asset;
    }

    private static AssetHistoryEvent assignmentEvent(Asset asset, Instant at) {
        AssetHistoryEvent event = new AssetHistoryEvent();
        event.setAsset(asset);
        event.setEventType(AssetHistoryEventType.ASSIGNMENT_CHANGE);
        event.setCreatedAt(at);
        event.setCreatedBy(UUID.randomUUID());
        return event;
    }
}
