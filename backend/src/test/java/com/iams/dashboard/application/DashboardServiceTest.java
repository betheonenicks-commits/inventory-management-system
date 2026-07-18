package com.iams.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.audit.application.AuditReportService;
import com.iams.audit.application.AuditService;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditRepository;
import com.iams.audit.domain.AuditStatus;
import com.iams.common.exception.ValidationFailedException;
import com.iams.dashboard.application.DashboardQueries.ExpiringEntry;
import com.iams.inventory.application.InventoryStockService;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.org.domain.OrgNode;
import com.iams.usr.application.OrgScopeGuard;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private DashboardQueries queries;
    @Mock private OrgScopeGuard scopeGuard;
    @Mock private AuditReportService auditReportService;
    @Mock private InventoryStockService stockService;
    @Mock private MaintenanceScheduleRepository maintenanceScheduleRepository;
    @Mock private AuditRepository auditRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(queries, scopeGuard, auditReportService, stockService,
                maintenanceScheduleRepository, auditRepository);
    }

    @Test
    void assetSummary_passesScopePrefixIntoEveryQuery() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn("/root/bldg-b");
        when(queries.assetCount("/root/bldg-b")).thenReturn(7L);
        when(queries.assetCountByCategory("/root/bldg-b"))
                .thenReturn(List.of(new DashboardQueries.LabelCount("IT Equipment", 5)));
        when(queries.assetCountByStatus("/root/bldg-b"))
                .thenReturn(List.of(new DashboardQueries.LabelCount("In Use", 7)));

        DashboardService.AssetSummary summary = service.assetSummary();

        assertThat(summary.totalAssets()).isEqualTo(7);
        assertThat(summary.byCategory()).extracting(DashboardQueries.LabelCount::label).containsExactly("IT Equipment");
        verify(queries).assetCount("/root/bldg-b");
        verify(queries).assetCountByCategory("/root/bldg-b");
        verify(queries).assetCountByStatus("/root/bldg-b");
    }

    @Test
    void auditCompletion_averagesPercentagesAndTreatsEmptyExpectedSetAsComplete() {
        when(auditReportService.dashboard()).thenReturn(List.of(
                dashboardItem("Audit A", 10, 4),   // 40%
                dashboardItem("Audit B", 10, 8),   // 80%
                dashboardItem("Audit C", 0, 0)));  // empty expected set -> 100%

        DashboardService.AuditCompletion completion = service.auditCompletion();

        assertThat(completion.audits()).extracting(DashboardService.AuditCompletionItem::percentComplete)
                .containsExactly(40, 80, 100);
        assertThat(completion.averagePercentComplete()).isEqualTo(73);
    }

    @Test
    void auditCompletion_emptyStateIsNullAverageNotZero() {
        when(auditReportService.dashboard()).thenReturn(List.of());

        DashboardService.AuditCompletion completion = service.auditCompletion();

        assertThat(completion.audits()).isEmpty();
        assertThat(completion.averagePercentComplete()).isNull();
        assertThat(completion.recentlyClosed()).isEmpty();
    }

    @Test
    void auditCompletion_surfacesExceptionCountsAndAveragesActiveAuditsOnly() {
        // US-AUD-17: active audits carry their exception count; the average is
        // over ACTIVE audits only, so recently-closed 100%s must not skew it.
        when(auditReportService.dashboard()).thenReturn(List.of(
                dashboardItem("Audit A", AuditStatus.IN_PROGRESS, 10, 4, 3),
                dashboardItem("Audit B", AuditStatus.PENDING_APPROVAL, 10, 8, 0)));
        when(auditReportService.recentlyClosed(anyInt())).thenReturn(List.of(
                dashboardItem("Closed X", AuditStatus.CLOSED, 5, 5, 1)));

        DashboardService.AuditCompletion completion = service.auditCompletion();

        assertThat(completion.audits())
                .extracting(DashboardService.AuditCompletionItem::status,
                        DashboardService.AuditCompletionItem::exceptionCount)
                .containsExactly(tuple("IN_PROGRESS", 3L), tuple("PENDING_APPROVAL", 0L));
        assertThat(completion.averagePercentComplete()).isEqualTo(60); // (40 + 80) / 2, closed audit excluded
        assertThat(completion.recentlyClosed())
                .extracting(DashboardService.AuditCompletionItem::name,
                        DashboardService.AuditCompletionItem::status,
                        DashboardService.AuditCompletionItem::exceptionCount)
                .containsExactly(tuple("Closed X", "CLOSED", 1L));
    }

    @Test
    void expirations_mergesAllThreeKindsSortedByNearestDate() {
        LocalDate today = LocalDate.now();
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        when(queries.warrantyExpirations(any(), any(), any())).thenReturn(List.of(
                new ExpiringEntry(UUID.randomUUID(), "Laptop", today.plusDays(20), null)));
        when(queries.insuranceExpirations(any(), any(), any())).thenReturn(List.of(
                new ExpiringEntry(UUID.randomUUID(), "Van", today.plusDays(5), "AcmeInsure")));
        MaintenanceSchedule overdue = schedule("Generator", today.minusDays(3));
        when(maintenanceScheduleRepository.findDueOnOrBefore(today.plusDays(30))).thenReturn(List.of(overdue));
        passThroughScopeFilter();

        List<DashboardService.Expiration> merged = service.expirations(30);

        assertThat(merged).extracting(DashboardService.Expiration::assetName)
                .containsExactly("Generator", "Van", "Laptop");
        assertThat(merged.get(0).kind()).isEqualTo(DashboardService.ExpirationKind.MAINTENANCE);
        assertThat(merged.get(0).dueDate()).isEqualTo(today.minusDays(3));
    }

    @Test
    void expirations_rejectsNegativeAndOversizedLookahead() {
        assertThatThrownBy(() -> service.expirations(-1)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.expirations(366)).isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(queries);
    }

    @Test
    void activityFeed_rejectsOutOfRangeLimit() {
        assertThatThrownBy(() -> service.activityFeed(0)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.activityFeed(101)).isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(queries);
    }

    @Test
    void activityFeed_passesScopeAndLimitThrough() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn("/root/bldg-b");
        when(queries.recentActivity("/root/bldg-b", 20)).thenReturn(List.of());

        service.activityFeed(20);

        verify(queries).recentActivity("/root/bldg-b", 20);
    }

    @Test
    void auditCalendar_scopedCallerOnlySeesAuditsInsideTheirSubtreeOrUnscopedOnes() {
        Audit inScope = calendarAudit("In scope", "/root/bldg-b/floor-1");
        Audit outOfScope = calendarAudit("Out of scope", "/root/bldg-a");
        Audit unscoped = calendarAudit("Category-scoped", null);
        when(auditRepository.findScheduledBetween(any(), any())).thenReturn(List.of(inScope, outOfScope, unscoped));
        when(scopeGuard.currentScopePathPrefix()).thenReturn("/root/bldg-b");

        List<Audit> visible = service.auditCalendar(30);

        assertThat(visible).extracting(Audit::getName).containsExactly("In scope", "Category-scoped");
    }

    @Test
    void auditCalendar_rejectsNegativeLookahead() {
        assertThatThrownBy(() -> service.auditCalendar(-5)).isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(auditRepository);
    }

    @SuppressWarnings("unchecked")
    private void passThroughScopeFilter() {
        lenient().when(scopeGuard.filterToScope(any(List.class), any(Function.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static AuditReportService.AuditDashboardItem dashboardItem(String name, long expected, long verified) {
        return dashboardItem(name, AuditStatus.IN_PROGRESS, expected, verified, 0);
    }

    private static AuditReportService.AuditDashboardItem dashboardItem(String name, AuditStatus status, long expected,
                                                                       long verified, long exceptionCount) {
        return new AuditReportService.AuditDashboardItem(UUID.randomUUID(), name, status,
                new AuditService.AuditProgress(expected, verified, 0, 0, 0), exceptionCount);
    }

    private static MaintenanceSchedule schedule(String assetName, LocalDate dueDate) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setName(assetName);
        OrgNode node = new OrgNode();
        node.setId(UUID.randomUUID());
        asset.setOrgNode(node);
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setAsset(asset);
        schedule.setNextDueDate(dueDate);
        schedule.setDescription("6-month service");
        return schedule;
    }

    private static Audit calendarAudit(String name, String scopePath) {
        Audit audit = new Audit();
        audit.setId(UUID.randomUUID());
        audit.setName(name);
        audit.setStatus(AuditStatus.IN_PROGRESS);
        audit.setScheduledDate(LocalDate.now().plusDays(10));
        if (scopePath != null) {
            OrgNode node = new OrgNode();
            node.setId(UUID.randomUUID());
            node.setPath(scopePath);
            audit.setScopeOrgNode(node);
        }
        return audit;
    }
}
