package com.iams.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.Vendor;
import com.iams.inventory.domain.VendorRepository;
import com.iams.procurement.domain.PurchaseOrder;
import com.iams.procurement.domain.PurchaseOrderRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private VendorService service;

    @BeforeEach
    void setUp() {
        service = new VendorService(vendorRepository, purchaseOrderRepository, currentUserProvider);
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "invmgr", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> service.create(" ", "a@b.com", "555-0100")).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_succeeds() {
        when(vendorRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        Vendor result = service.create("Acme Supplies", "sales@acme.example", "555-0100");

        assertThat(result.getName()).isEqualTo("Acme Supplies");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void deactivate_flagsRatherThanDeletes() {
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setActive(true);
        when(vendorRepository.findById(vendor.getId())).thenReturn(Optional.of(vendor));
        when(vendorRepository.saveAndFlush(vendor)).thenReturn(vendor);

        Vendor result = service.deactivate(vendor.getId());

        assertThat(result.isActive()).isFalse();
        assertThat(result.getId()).isEqualTo(vendor.getId());
    }

    @Test
    void purchaseHistory_rejectsUnknownVendor() {
        UUID unknownId = UUID.randomUUID();
        when(vendorRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> service.purchaseHistory(unknownId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void purchaseHistory_returnsEveryLinkedPurchaseOrder() {
        UUID vendorId = UUID.randomUUID();
        PurchaseOrder order = new PurchaseOrder();
        order.setId(UUID.randomUUID());
        when(vendorRepository.existsById(vendorId)).thenReturn(true);
        when(purchaseOrderRepository.findByVendorIdWithRequestOrderByCreatedAtDesc(vendorId)).thenReturn(List.of(order));

        List<PurchaseOrder> result = service.purchaseHistory(vendorId);

        assertThat(result).containsExactly(order);
    }
}
