package com.iams.maintenance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.maintenance.domain.RepairEvent;
import com.iams.maintenance.domain.RepairEventRepository;
import com.iams.maintenance.domain.RepairEventStatus;
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
class RepairServiceTest {

    @Mock private RepairEventRepository repairEventRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetStatusDefRepository statusDefRepository;
    @Mock private AssetHistoryRecorder historyRecorder;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;

    private RepairService service;
    private UUID actorId;
    private Asset asset;
    private AssetStatusDef inUse;
    private AssetStatusDef underRepair;

    @BeforeEach
    void setUp() {
        service = new RepairService(repairEventRepository, assetRepository, statusDefRepository, historyRecorder,
                currentUserProvider, scopeGuard);
        actorId = UUID.randomUUID();

        inUse = new AssetStatusDef();
        inUse.setId(UUID.randomUUID());
        inUse.setCode("IN_USE");
        underRepair = new AssetStatusDef();
        underRepair.setId(UUID.randomUUID());
        underRepair.setCode("UNDER_REPAIR");

        asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setStatus(inUse);
        OrgNode orgNode = new OrgNode();
        orgNode.setId(UUID.randomUUID());
        asset.setOrgNode(orgNode);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void open_rejectsBlankReason() {
        assertThatThrownBy(() -> service.open(new OpenRepairCommand(asset.getId(), "Acme Repairs", " ",
                BigDecimal.TEN, LocalDate.now().plusDays(7))))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void open_capturesPreviousStatusAndMovesToUnderRepair() {
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));
        when(statusDefRepository.findByCode("UNDER_REPAIR")).thenReturn(Optional.of(underRepair));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.eq(asset), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AssetHistoryEvent());
        when(repairEventRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        RepairEvent result = service.open(new OpenRepairCommand(asset.getId(), "Acme Repairs", "Screen cracked",
                BigDecimal.TEN, LocalDate.now().plusDays(7)));

        assertThat(result.getPreviousStatusCode()).isEqualTo("IN_USE");
        assertThat(result.getStatus()).isEqualTo(RepairEventStatus.OPEN);
        assertThat(asset.getStatus()).isEqualTo(underRepair);
    }

    @Test
    void close_requiresActualReturnDate() {
        RepairEvent event = openEvent();
        when(repairEventRepository.findByIdWithAsset(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.close(event.getId(), new CloseRepairCommand(null, BigDecimal.ONE)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void close_revertsAssetToPreviousStatus() {
        RepairEvent event = openEvent();
        asset.setStatus(underRepair);
        when(repairEventRepository.findByIdWithAsset(event.getId())).thenReturn(Optional.of(event));
        when(statusDefRepository.findByCode("IN_USE")).thenReturn(Optional.of(inUse));
        when(assetRepository.saveAndFlush(asset)).thenReturn(asset);
        when(historyRecorder.record(org.mockito.ArgumentMatchers.eq(asset), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AssetHistoryEvent());
        when(repairEventRepository.saveAndFlush(event)).thenReturn(event);

        RepairEvent result = service.close(event.getId(), new CloseRepairCommand(LocalDate.now(), new BigDecimal("45.00")));

        assertThat(result.getStatus()).isEqualTo(RepairEventStatus.CLOSED);
        assertThat(result.getActualCost()).isEqualByComparingTo("45.00");
        assertThat(asset.getStatus()).isEqualTo(inUse);
    }

    @Test
    void close_rejectsAlreadyClosedEvent() {
        RepairEvent event = openEvent();
        event.setStatus(RepairEventStatus.CLOSED);
        when(repairEventRepository.findByIdWithAsset(event.getId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.close(event.getId(), new CloseRepairCommand(LocalDate.now(), null)))
                .isInstanceOf(ConflictException.class);
    }

    private RepairEvent openEvent() {
        RepairEvent event = new RepairEvent();
        event.setId(UUID.randomUUID());
        event.setAsset(asset);
        event.setPreviousStatusCode("IN_USE");
        event.setReason("Screen cracked");
        event.setStatus(RepairEventStatus.OPEN);
        event.setLoggedBy(actorId);
        return event;
    }
}
