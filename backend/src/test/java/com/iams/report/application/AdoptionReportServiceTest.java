package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.analytics.domain.AdoptionAggregate;
import com.iams.analytics.domain.UsageEventRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdoptionReportServiceTest {

    @Mock private UsageEventRepository usageEventRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleAssignmentRepository assignmentRepository;

    private AdoptionReportService service;

    @BeforeEach
    void setUp() {
        service = new AdoptionReportService(usageEventRepository, roleRepository, assignmentRepository);
        lenient().when(usageEventRepository.aggregateSince(any())).thenReturn(List.of());
    }

    private Role role(String code, boolean held, String... permissions) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setPermissions(new ArrayList<>(List.of(permissions)));
        lenient().when(assignmentRepository.findByRoleId(role.getId()))
                .thenReturn(held ? List.of(new UserRoleAssignment()) : List.of());
        return role;
    }

    private List<String> row(TabularReport report, String roleCode, String module) {
        return report.rows().stream()
                .filter(r -> r.get(0).equals(roleCode) && r.get(1).equals(module))
                .findFirst().orElse(null);
    }

    @Test
    void expectedModuleWithZeroUsage_producesAVisibleGapRow() {
        Role auditor = role("AUDITOR", true, "assets:read", "audits:read");
        when(roleRepository.findAll()).thenReturn(List.of(auditor));

        TabularReport report = service.usageAdoption(90);

        assertThat(row(report, "AUDITOR", "audits")).isNotNull()
                .satisfies(r -> {
                    assertThat(r.get(2)).isEqualTo("0");
                    assertThat(r.get(5)).contains("GAP");
                });
        // search has no permission gate - every held role is expected there.
        assertThat(row(report, "AUDITOR", "search").get(5)).contains("GAP");
    }

    @Test
    void nearZeroUsageOfAnExpectedModule_isFlaggedNotAveragedAway() {
        Role auditor = role("AUDITOR", true, "audits:read");
        when(roleRepository.findAll()).thenReturn(List.of(auditor));
        when(usageEventRepository.aggregateSince(any())).thenReturn(List.of(
                new AdoptionAggregate("AUDITOR", "audits", 3, 1, Instant.now())));

        TabularReport report = service.usageAdoption(90);

        assertThat(row(report, "AUDITOR", "audits").get(5)).contains("NEAR-ZERO");
    }

    @Test
    void healthyUsage_isOk() {
        Role auditor = role("AUDITOR", true, "audits:read");
        when(roleRepository.findAll()).thenReturn(List.of(auditor));
        when(usageEventRepository.aggregateSince(any())).thenReturn(List.of(
                new AdoptionAggregate("AUDITOR", "audits", 42, 3, Instant.now())));

        TabularReport report = service.usageAdoption(90);

        List<String> row = row(report, "AUDITOR", "audits");
        assertThat(row.get(2)).isEqualTo("42");
        assertThat(row.get(3)).isEqualTo("3");
        assertThat(row.get(5)).isEqualTo("OK");
    }

    @Test
    void moduleNeitherExpectedNorUsed_isOmittedEntirely() {
        Role auditor = role("AUDITOR", true, "audits:read");
        when(roleRepository.findAll()).thenReturn(List.of(auditor));

        TabularReport report = service.usageAdoption(90);

        assertThat(row(report, "AUDITOR", "inventory")).isNull();
    }

    @Test
    void wildcardRole_isExpectedInEveryTrackedModule() {
        Role superAdmin = role("SUPER_ADMIN", true, "*");
        when(roleRepository.findAll()).thenReturn(List.of(superAdmin));

        TabularReport report = service.usageAdoption(90);

        for (String module : List.of("search", "dashboard", "assets", "audits", "inventory", "reports")) {
            assertThat(row(report, "SUPER_ADMIN", module)).as(module).isNotNull()
                    .satisfies(r -> assertThat(r.get(5)).contains("GAP"));
        }
    }

    @Test
    void roleNobodyHolds_doesNotAppear() {
        Role auditor = role("AUDITOR", true, "audits:read");
        Role unheld = role("INTEGRATION_SERVICE", false, "audits:read");
        when(roleRepository.findAll()).thenReturn(List.of(auditor, unheld));

        TabularReport report = service.usageAdoption(90);

        assertThat(report.rows()).noneMatch(r -> r.get(0).equals("INTEGRATION_SERVICE"));
    }

    @Test
    void observedModuleOutsideTheExpectationCatalog_stillAppears() {
        Role auditor = role("AUDITOR", true, "audits:read");
        when(roleRepository.findAll()).thenReturn(List.of(auditor));
        when(usageEventRepository.aggregateSince(any())).thenReturn(List.of(
                new AdoptionAggregate("AUDITOR", "feedback", 2, 1, Instant.now())));

        TabularReport report = service.usageAdoption(90);

        List<String> row = row(report, "AUDITOR", "feedback");
        assertThat(row).isNotNull();
        assertThat(row.get(2)).isEqualTo("2");
        assertThat(row.get(5)).isEqualTo("-");
    }

    @Test
    void outOfRangePeriod_isRejected() {
        assertThatThrownBy(() -> service.usageAdoption(0)).isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.usageAdoption(731)).isInstanceOf(ValidationFailedException.class);
    }
}
