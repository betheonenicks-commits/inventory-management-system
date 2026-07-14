package com.iams.maintenance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.maintenance.domain.MaintenanceEvent;
import com.iams.maintenance.domain.MaintenanceEventRepository;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.maintenance.domain.MaintenanceType;
import com.iams.org.domain.OrgNode;
import com.iams.usr.application.OrgScopeGuard;
import java.math.BigDecimal;
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
class MaintenanceEventServiceTest {

    @Mock private MaintenanceEventRepository eventRepository;
    @Mock private MaintenanceScheduleRepository scheduleRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;

    private MaintenanceEventService service;
    private Asset asset;

    @BeforeEach
    void setUp() {
        service = new MaintenanceEventService(eventRepository, scheduleRepository, assetRepository, currentUserProvider, scopeGuard);
        asset = new Asset();
        asset.setId(UUID.randomUUID());
        OrgNode orgNode = new OrgNode();
        orgNode.setId(UUID.randomUUID());
        asset.setOrgNode(orgNode);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void recordPreventive_advancesScheduleFromItsOwnPriorDueDate() {
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAsset(asset);
        schedule.setIntervalMonths(6);
        LocalDate originalDueDate = LocalDate.of(2026, 7, 1);
        schedule.setNextDueDate(originalDueDate);
        schedule.setActive(true);
        when(scheduleRepository.findByIdWithAsset(schedule.getId())).thenReturn(Optional.of(schedule));
        when(eventRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.saveAndFlush(schedule)).thenReturn(schedule);

        MaintenanceEvent result = service.recordPreventive(schedule.getId(), "Filters replaced", new BigDecimal("120.00"));

        assertThat(result.getMaintenanceType()).isEqualTo(MaintenanceType.PREVENTIVE);
        assertThat(result.getSchedule()).isEqualTo(schedule);
        assertThat(schedule.getNextDueDate()).isEqualTo(originalDueDate.plusMonths(6));
    }

    @Test
    void recordPreventive_rejectsInactiveSchedule() {
        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setAsset(asset);
        schedule.setActive(false);
        when(scheduleRepository.findByIdWithAsset(schedule.getId())).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.recordPreventive(schedule.getId(), "notes", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordCorrective_requiresRootCauseNote() {
        assertThatThrownBy(() -> service.recordCorrective(asset.getId(), " ", null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void recordCorrective_createsEventWithNoScheduleLink() {
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(eventRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceEvent result = service.recordCorrective(asset.getId(), "Fan bearing failed", new BigDecimal("80.00"));

        assertThat(result.getMaintenanceType()).isEqualTo(MaintenanceType.CORRECTIVE);
        assertThat(result.getSchedule()).isNull();
        assertThat(result.getNotes()).isEqualTo("Fan bearing failed");
    }
}
