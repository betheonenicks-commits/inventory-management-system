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

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(assetRepository, orgNodeRepository, personRepository,
                securityEventLogRepository, reportQueries, dashboardService, scopeGuard);
    }

    @Test
    void assetRegister_walksEveryPageIntoOneReport() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        Asset a1 = asset("AST-1", "Laptop");
        Asset a2 = asset("AST-2", "Projector");
        when(assetRepository.search(eq(null), eq(null), eq(null), eq(null), any()))
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
        when(assetRepository.search(any(), any(), any(), eq("/root/bldg-a/"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        TabularReport report = service.assetRegister(nodeId, null, null);

        assertThat(report.title()).contains("Building A");
        verify(assetRepository).search(eq(null), eq(null), eq(null), eq("/root/bldg-a/"), any());
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
