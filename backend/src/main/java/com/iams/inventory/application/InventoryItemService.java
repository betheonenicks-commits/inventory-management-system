package com.iams.inventory.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.CostingMethod;
import com.iams.inventory.domain.InventoryItem;
import com.iams.inventory.domain.InventoryItemRepository;
import com.iams.inventory.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-INV-01/06/11: the item catalog - what's tracked by quantity, its unit of measure, reorder level, and costing method. */
@Service
public class InventoryItemService {

    private final InventoryItemRepository itemRepository;
    private final CurrentUserProvider currentUserProvider;

    public InventoryItemService(InventoryItemRepository itemRepository, CurrentUserProvider currentUserProvider) {
        this.itemRepository = itemRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public InventoryItem create(String name, String sku, UnitOfMeasure unitOfMeasure, BigDecimal reorderLevel,
                                 CostingMethod costingMethod) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "An item name is required");
        }
        if (sku == null || sku.isBlank()) {
            throw ValidationFailedException.singleField("sku", "A SKU is required");
        }
        if (itemRepository.findBySku(sku).isPresent()) {
            throw new ConflictException("SKU_TAKEN", "An item with SKU '" + sku + "' already exists");
        }
        if (reorderLevel != null && reorderLevel.signum() < 0) {
            throw ValidationFailedException.singleField("reorderLevel", "Cannot be negative");
        }

        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setSku(sku);
        item.setUnitOfMeasure(unitOfMeasure);
        item.setReorderLevel(reorderLevel);
        // AC-INV-06-H: takes effect prospectively - a later change() call, not this one, is what "the change itself is recorded" governs.
        item.setCostingMethod(costingMethod != null ? costingMethod : CostingMethod.WEIGHTED_AVERAGE);
        item.setActive(true);
        item.setCreatedBy(currentUserProvider.current().id());
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public InventoryItem get(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> NotFoundException.of("InventoryItem", id));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> list(boolean activeOnly) {
        return activeOnly ? itemRepository.findByActiveTrueOrderByNameAsc() : itemRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public InventoryItem update(UUID id, String name, BigDecimal reorderLevel, CostingMethod costingMethod) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "An item name is required");
        }
        if (reorderLevel != null && reorderLevel.signum() < 0) {
            throw ValidationFailedException.singleField("reorderLevel", "Cannot be negative");
        }
        InventoryItem item = get(id);
        item.setName(name);
        item.setReorderLevel(reorderLevel);
        if (costingMethod != null) {
            // AC-INV-06-H: prospective only - already-recorded average_unit_cost balances are untouched by this switch.
            item.setCostingMethod(costingMethod);
        }
        item.setUpdatedBy(currentUserProvider.current().id());
        return itemRepository.saveAndFlush(item);
    }

    @Transactional
    public InventoryItem deactivate(UUID id) {
        InventoryItem item = get(id);
        item.setActive(false);
        item.setUpdatedBy(currentUserProvider.current().id());
        return itemRepository.saveAndFlush(item);
    }
}
