package com.iams.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.InventoryStockBalanceRepository;
import com.iams.inventory.domain.Warehouse;
import com.iams.inventory.domain.WarehouseRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock private WarehouseRepository warehouseRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private InventoryStockBalanceRepository balanceRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private OrgScopeGuard scopeGuard;

    private WarehouseService service;
    private UUID actorId;
    private OrgNode orgNode;

    @BeforeEach
    void setUp() {
        service = new WarehouseService(warehouseRepository, orgNodeRepository, balanceRepository, currentUserProvider, scopeGuard);
        actorId = UUID.randomUUID();
        orgNode = new OrgNode();
        orgNode.setId(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "invmgr", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(" ", "WH-1", orgNode.getId())).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsBlankCode() {
        assertThatThrownBy(() -> service.create("Main Warehouse", " ", orgNode.getId())).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsDuplicateCode() {
        when(warehouseRepository.findByCode("WH-1")).thenReturn(Optional.of(new Warehouse()));

        assertThatThrownBy(() -> service.create("Main Warehouse", "WH-1", orgNode.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsUnknownOrgNode() {
        UUID unknownOrgNodeId = UUID.randomUUID();
        when(warehouseRepository.findByCode("WH-1")).thenReturn(Optional.empty());
        when(orgNodeRepository.findById(unknownOrgNodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Main Warehouse", "WH-1", unknownOrgNodeId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_succeeds() {
        when(warehouseRepository.findByCode("WH-1")).thenReturn(Optional.empty());
        when(orgNodeRepository.findById(orgNode.getId())).thenReturn(Optional.of(orgNode));
        when(warehouseRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        Warehouse result = service.create("Main Warehouse", "WH-1", orgNode.getId());

        assertThat(result.getName()).isEqualTo("Main Warehouse");
        assertThat(result.getCode()).isEqualTo("WH-1");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void deactivate_blockedWhileStockOnHand() {
        Warehouse warehouse = warehouseWithOrgNode();
        when(warehouseRepository.findByIdWithOrgNode(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(balanceRepository.existsByWarehouseIdAndQuantityOnHandGreaterThan(warehouse.getId(), BigDecimal.ZERO)).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(warehouse.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivate_succeeds_whenNoStockOnHand() {
        Warehouse warehouse = warehouseWithOrgNode();
        when(warehouseRepository.findByIdWithOrgNode(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(balanceRepository.existsByWarehouseIdAndQuantityOnHandGreaterThan(warehouse.getId(), BigDecimal.ZERO)).thenReturn(false);
        when(warehouseRepository.saveAndFlush(warehouse)).thenReturn(warehouse);

        Warehouse result = service.deactivate(warehouse.getId());

        assertThat(result.isActive()).isFalse();
    }

    private Warehouse warehouseWithOrgNode() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setOrgNode(orgNode);
        warehouse.setActive(true);
        return warehouse;
    }
}
