package com.iams.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.CostingMethod;
import com.iams.inventory.domain.InventoryItem;
import com.iams.inventory.domain.InventoryItemCostingMethodChange;
import com.iams.inventory.domain.InventoryItemCostingMethodChangeRepository;
import com.iams.inventory.domain.InventoryItemRepository;
import com.iams.inventory.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock private InventoryItemRepository itemRepository;
    @Mock private InventoryItemCostingMethodChangeRepository costingMethodChangeRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private InventoryItemService service;

    @BeforeEach
    void setUp() {
        service = new InventoryItemService(itemRepository, costingMethodChangeRepository, currentUserProvider);
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "invmgr", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsDuplicateSku() {
        when(itemRepository.findBySku("GLV-001")).thenReturn(Optional.of(new InventoryItem()));

        assertThatThrownBy(() -> service.create("Nitrile Gloves", "GLV-001", UnitOfMeasure.BOX, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsNegativeReorderLevel() {
        when(itemRepository.findBySku("GLV-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Nitrile Gloves", "GLV-001", UnitOfMeasure.BOX, new BigDecimal("-5"), null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_defaultsToWeightedAverageCosting() {
        when(itemRepository.findBySku("GLV-001")).thenReturn(Optional.empty());
        when(itemRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.create("Nitrile Gloves", "GLV-001", UnitOfMeasure.BOX, new BigDecimal("20"), null);

        assertThat(result.getCostingMethod()).isEqualTo(CostingMethod.WEIGHTED_AVERAGE);
        assertThat(result.getUnitOfMeasure()).isEqualTo(UnitOfMeasure.BOX);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void update_appliesCostingMethodChange() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.saveAndFlush(item)).thenReturn(item);

        InventoryItem result = service.update(item.getId(), "Nitrile Gloves (Large)", new BigDecimal("25"), CostingMethod.LAST_COST);

        assertThat(result.getCostingMethod()).isEqualTo(CostingMethod.LAST_COST);
        assertThat(result.getReorderLevel()).isEqualByComparingTo("25");
    }

    @Test
    void update_recordsCostingMethodChange_whenMethodActuallyChanges() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        UUID actorId = UUID.randomUUID();
        org.mockito.Mockito.when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "invmgr", Set.of("INVENTORY_MANAGER")));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.saveAndFlush(item)).thenReturn(item);

        service.update(item.getId(), "Nitrile Gloves", null, CostingMethod.LAST_COST);

        ArgumentCaptor<InventoryItemCostingMethodChange> captor = ArgumentCaptor.forClass(InventoryItemCostingMethodChange.class);
        org.mockito.Mockito.verify(costingMethodChangeRepository).save(captor.capture());
        assertThat(captor.getValue().getOldMethod()).isEqualTo(CostingMethod.WEIGHTED_AVERAGE);
        assertThat(captor.getValue().getNewMethod()).isEqualTo(CostingMethod.LAST_COST);
        assertThat(captor.getValue().getChangedBy()).isEqualTo(actorId);
    }

    @Test
    void update_doesNotRecordChange_whenResubmittingTheSameMethod() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.saveAndFlush(item)).thenReturn(item);

        service.update(item.getId(), "Nitrile Gloves", null, CostingMethod.WEIGHTED_AVERAGE);

        org.mockito.Mockito.verifyNoInteractions(costingMethodChangeRepository);
    }

    @Test
    void update_doesNotRecordChange_whenCostingMethodOmitted() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.saveAndFlush(item)).thenReturn(item);

        InventoryItem result = service.update(item.getId(), "Nitrile Gloves", new BigDecimal("30"), null);

        assertThat(result.getCostingMethod()).isEqualTo(CostingMethod.WEIGHTED_AVERAGE);
        org.mockito.Mockito.verifyNoInteractions(costingMethodChangeRepository);
    }

    @Test
    void costingMethodHistory_rejectsUnknownItem() {
        UUID unknownId = UUID.randomUUID();
        when(itemRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> service.costingMethodHistory(unknownId))
                .isInstanceOf(com.iams.common.exception.NotFoundException.class);
    }

    @Test
    void costingMethodHistory_returnsChangesOldestFirst() {
        UUID itemId = UUID.randomUUID();
        InventoryItemCostingMethodChange change = new InventoryItemCostingMethodChange();
        when(itemRepository.existsById(itemId)).thenReturn(true);
        when(costingMethodChangeRepository.findByInventoryItemIdOrderByChangedAtAsc(itemId)).thenReturn(List.of(change));

        List<InventoryItemCostingMethodChange> result = service.costingMethodHistory(itemId);

        assertThat(result).containsExactly(change);
    }
}
