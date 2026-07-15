package com.iams.inventory.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.InventoryStockBalanceRepository;
import com.iams.inventory.domain.Warehouse;
import com.iams.inventory.domain.WarehouseRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-INV-03: warehouse CRUD, org-scoped exactly like {@code Asset}. */
@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final InventoryStockBalanceRepository balanceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public WarehouseService(WarehouseRepository warehouseRepository, OrgNodeRepository orgNodeRepository,
                             InventoryStockBalanceRepository balanceRepository, CurrentUserProvider currentUserProvider,
                             OrgScopeGuard scopeGuard) {
        this.warehouseRepository = warehouseRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.balanceRepository = balanceRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional
    public Warehouse create(String name, String code, UUID orgNodeId) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "A warehouse name is required");
        }
        if (code == null || code.isBlank()) {
            throw ValidationFailedException.singleField("code", "A warehouse code is required");
        }
        if (warehouseRepository.findByCode(code).isPresent()) {
            throw new ConflictException("WAREHOUSE_CODE_TAKEN", "A warehouse with code '" + code + "' already exists");
        }
        OrgNode orgNode = orgNodeRepository.findById(orgNodeId).orElseThrow(() -> NotFoundException.of("OrgNode", orgNodeId));

        UUID actor = currentUserProvider.current().id();
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setCode(code);
        warehouse.setOrgNode(orgNode);
        warehouse.setActive(true);
        warehouse.setCreatedBy(actor);
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public Warehouse get(UUID id) {
        Warehouse warehouse = warehouseRepository.findByIdWithOrgNode(id).orElseThrow(() -> NotFoundException.of("Warehouse", id));
        scopeGuard.requireWithinScope(warehouse.getOrgNode().getId(), "warehouse", id);
        return warehouse;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> list() {
        List<Warehouse> warehouses = warehouseRepository.findAllWithOrgNodeOrderByNameAsc();
        return scopeGuard.filterToScope(warehouses, w -> w.getOrgNode().getId());
    }

    @Transactional
    public Warehouse update(UUID id, String name) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "A warehouse name is required");
        }
        Warehouse warehouse = get(id);
        warehouse.setName(name);
        warehouse.setUpdatedBy(currentUserProvider.current().id());
        return warehouseRepository.saveAndFlush(warehouse);
    }

    /** AC-INV-03-X: blocked until stock is moved out or transferred - never a forced/cascading deactivation. */
    @Transactional
    public Warehouse deactivate(UUID id) {
        Warehouse warehouse = get(id);
        if (balanceRepository.existsByWarehouseIdAndQuantityOnHandGreaterThan(id, BigDecimal.ZERO)) {
            throw new ConflictException("WAREHOUSE_HAS_STOCK",
                    "This warehouse still has stock on hand - move or transfer it out before deactivating");
        }
        warehouse.setActive(false);
        warehouse.setUpdatedBy(currentUserProvider.current().id());
        return warehouseRepository.saveAndFlush(warehouse);
    }
}
