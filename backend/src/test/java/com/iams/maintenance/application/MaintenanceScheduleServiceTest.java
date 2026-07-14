package com.iams.maintenance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.org.domain.OrgNode;
import com.iams.usr.application.OrgScopeGuard;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaintenanceScheduleServiceTest {

    @Mock private MaintenanceScheduleRepository scheduleRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;

    private MaintenanceScheduleService service;
    private Asset asset;

    @BeforeEach
    void setUp() {
        service = new MaintenanceScheduleService(scheduleRepository, assetRepository, currentUserProvider, scopeGuard);
        asset = new Asset();
        asset.setId(UUID.randomUUID());
        OrgNode orgNode = new OrgNode();
        orgNode.setId(UUID.randomUUID());
        asset.setOrgNode(orgNode);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsNonPositiveInterval() {
        assertThatThrownBy(() -> service.create(asset.getId(), 0, LocalDate.now().plusMonths(6), "HVAC check"))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_succeeds() {
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(scheduleRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceSchedule result = service.create(asset.getId(), 6, LocalDate.now().plusMonths(6), "HVAC check");

        assertThat(result.getIntervalMonths()).isEqualTo(6);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void deactivate_setsActiveFalse() {
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAsset(asset);
        schedule.setActive(true);
        when(scheduleRepository.findByIdWithAsset(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.saveAndFlush(schedule)).thenReturn(schedule);

        MaintenanceSchedule result = service.deactivate(schedule.getId());

        assertThat(result.isActive()).isFalse();
    }
}
