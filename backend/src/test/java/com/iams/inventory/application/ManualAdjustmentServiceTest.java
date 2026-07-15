package com.iams.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.InventoryItem;
import com.iams.inventory.domain.InventoryItemRepository;
import com.iams.inventory.domain.InventoryManualAdjustmentRequest;
import com.iams.inventory.domain.InventoryManualAdjustmentRequestRepository;
import com.iams.inventory.domain.InventoryStockBalance;
import com.iams.inventory.domain.InventoryStockBalanceRepository;
import com.iams.inventory.domain.InventoryTransaction;
import com.iams.inventory.domain.InventoryTransactionRepository;
import com.iams.inventory.domain.Warehouse;
import com.iams.inventory.domain.WarehouseRepository;
import com.iams.lifecycle.application.ApprovalRoutingService;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.usr.domain.AppUserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ManualAdjustmentServiceTest {

    @Mock private InventoryManualAdjustmentRequestRepository requestRepository;
    @Mock private InventoryItemRepository itemRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private InventoryStockBalanceRepository balanceRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private ApprovalRoutingService routingService;
    @Mock private CurrentUserProvider currentUserProvider;

    private ManualAdjustmentService service;
    private UUID actorId;
    private InventoryItem item;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new ManualAdjustmentService(requestRepository, itemRepository, warehouseRepository, balanceRepository,
                transactionRepository, appUserRepository, routingService, currentUserProvider);
        actorId = UUID.randomUUID();
        item = new InventoryItem();
        item.setId(UUID.randomUUID());
        warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "invmgr", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void request_rejectsBlankReason() {
        assertThatThrownBy(() -> service.request(item.getId(), warehouse.getId(), null, null, new BigDecimal("-3"), " ", UUID.randomUUID()))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void request_rejectsZeroDelta() {
        assertThatThrownBy(() -> service.request(item.getId(), warehouse.getId(), null, null, BigDecimal.ZERO, "Recount", UUID.randomUUID()))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void request_succeeds_asPending() {
        UUID approverId = UUID.randomUUID();
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        when(requestRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryManualAdjustmentRequest result = service.request(item.getId(), warehouse.getId(), null, null,
                new BigDecimal("-3"), "Recount found 3 fewer units", approverId);

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.PENDING);
        assertThat(result.getQuantityDelta()).isEqualByComparingTo("-3");
    }

    @Test
    void approve_rejectsNonRoutedApprover() {
        InventoryManualAdjustmentRequest request = pendingRequest();
        when(requestRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(request.getNominalApproverId())).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approve_rejectsWhenResultWouldGoNegative() {
        InventoryManualAdjustmentRequest request = pendingRequest();
        request.setQuantityDelta(new BigDecimal("-10"));
        request.setNominalApproverId(actorId);
        when(requestRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(actorId)).thenReturn(actorId);
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setQuantityOnHand(new BigDecimal("3"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouse.getId(), "", ""))
                .thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void approve_appliesDelta_andRecordsTransaction() {
        InventoryManualAdjustmentRequest request = pendingRequest();
        request.setQuantityDelta(new BigDecimal("-3"));
        request.setNominalApproverId(actorId);
        when(requestRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(actorId)).thenReturn(actorId);
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setQuantityOnHand(new BigDecimal("50"));
        when(balanceRepository.findByInventoryItemIdAndWarehouseIdAndSubLocationAndLotNumber(item.getId(), warehouse.getId(), "", ""))
                .thenReturn(Optional.of(balance));
        when(balanceRepository.saveAndFlush(balance)).thenReturn(balance);
        InventoryTransaction savedTransaction = new InventoryTransaction();
        savedTransaction.setId(UUID.randomUUID());
        when(transactionRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(savedTransaction);
        when(requestRepository.saveAndFlush(request)).thenReturn(request);

        InventoryManualAdjustmentRequest result = service.approve(request.getId());

        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo("47");
        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.APPROVED);
        assertThat(result.getResultingTransactionId()).isEqualTo(savedTransaction.getId());
    }

    @Test
    void reject_requiresReason() {
        InventoryManualAdjustmentRequest request = pendingRequest();
        request.setNominalApproverId(actorId);

        assertThatThrownBy(() -> service.reject(request.getId(), " ")).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void reject_succeeds() {
        InventoryManualAdjustmentRequest request = pendingRequest();
        request.setNominalApproverId(actorId);
        when(requestRepository.findByIdWithAssociations(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(actorId)).thenReturn(actorId);
        when(requestRepository.saveAndFlush(request)).thenReturn(request);

        InventoryManualAdjustmentRequest result = service.reject(request.getId(), "Insufficient evidence for recount");

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Insufficient evidence for recount");
    }

    private InventoryManualAdjustmentRequest pendingRequest() {
        InventoryManualAdjustmentRequest request = new InventoryManualAdjustmentRequest();
        request.setId(UUID.randomUUID());
        request.setInventoryItem(item);
        request.setWarehouse(warehouse);
        request.setSubLocation("");
        request.setLotNumber("");
        request.setQuantityDelta(new BigDecimal("-3"));
        request.setReason("Recount");
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(UUID.randomUUID());
        return request;
    }
}
