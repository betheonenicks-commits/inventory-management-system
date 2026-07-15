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
import com.iams.inventory.domain.InventoryItemRepository;
import com.iams.inventory.domain.UnitOfMeasure;
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
class InventoryItemServiceTest {

    @Mock private InventoryItemRepository itemRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private InventoryItemService service;

    @BeforeEach
    void setUp() {
        service = new InventoryItemService(itemRepository, currentUserProvider);
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
    void update_prospectiveCostingMethodChange_doesNotTouchNullValue() {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.saveAndFlush(item)).thenReturn(item);

        InventoryItem result = service.update(item.getId(), "Nitrile Gloves (Large)", new BigDecimal("25"), CostingMethod.LAST_COST);

        assertThat(result.getCostingMethod()).isEqualTo(CostingMethod.LAST_COST);
        assertThat(result.getReorderLevel()).isEqualByComparingTo("25");
    }
}
