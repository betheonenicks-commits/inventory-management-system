package com.iams.procurement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.procurement.domain.PurchaseOrder;
import com.iams.procurement.domain.PurchaseOrderLine;
import com.iams.procurement.domain.PurchaseOrderLineEventRepository;
import com.iams.procurement.domain.PurchaseOrderLineRepository;
import com.iams.procurement.domain.PurchaseOrderLineStatus;
import com.iams.procurement.domain.PurchaseOrderRepository;
import com.iams.procurement.domain.PurchaseOrderStatus;
import com.iams.procurement.domain.PurchaseRequest;
import com.iams.procurement.domain.PurchaseRequestRepository;
import com.iams.procurement.domain.service.PurchaseOrderNumberGenerator;
import java.math.BigDecimal;
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
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository orderRepository;
    @Mock private PurchaseOrderLineRepository lineRepository;
    @Mock private PurchaseOrderLineEventRepository lineEventRepository;
    @Mock private PurchaseRequestRepository requestRepository;
    @Mock private PurchaseOrderNumberGenerator numberGenerator;
    @Mock private CurrentUserProvider currentUserProvider;

    private PurchaseOrderService service;
    private UUID actorId;
    private PurchaseRequest approvedRequest;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(orderRepository, lineRepository, lineEventRepository, requestRepository,
                numberGenerator, currentUserProvider);
        actorId = UUID.randomUUID();
        approvedRequest = new PurchaseRequest();
        approvedRequest.setId(UUID.randomUUID());
        approvedRequest.setStatus(LifecycleRequestStatus.APPROVED);

        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsUnapprovedRequest() {
        PurchaseRequest pending = new PurchaseRequest();
        pending.setId(UUID.randomUUID());
        pending.setStatus(LifecycleRequestStatus.PENDING);
        when(requestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.create(new PurchaseOrderCreateCommand(pending.getId(), "Dell",
                List.of(new PurchaseOrderLineCommand("Laptop", 5, BigDecimal.TEN)))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsEmptyLines() {
        assertThatThrownBy(() -> service.create(new PurchaseOrderCreateCommand(UUID.randomUUID(), "Dell", List.of())))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_succeeds() {
        when(requestRepository.findById(approvedRequest.getId())).thenReturn(Optional.of(approvedRequest));
        when(numberGenerator.next()).thenReturn("PO-2026-000001");
        when(orderRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder result = service.create(new PurchaseOrderCreateCommand(approvedRequest.getId(), "Dell",
                List.of(new PurchaseOrderLineCommand("Laptop", 5, new BigDecimal("999.00")))));

        assertThat(result.getPoNumber()).isEqualTo("PO-2026-000001");
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
        assertThat(result.getPurchaseRequest()).isEqualTo(approvedRequest);
    }

    @Test
    void receive_rejectsOverReceipt() {
        PurchaseOrderLine line = openLine(10);
        when(lineRepository.findByIdWithOrder(line.getId())).thenReturn(Optional.of(line));

        assertThatThrownBy(() -> service.receive(line.getId(), 11, null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void receive_marksPartiallyReceived_whenLessThanOrdered() {
        PurchaseOrderLine line = openLine(10);
        when(lineRepository.findByIdWithOrder(line.getId())).thenReturn(Optional.of(line));
        when(lineRepository.saveAndFlush(line)).thenReturn(line);
        when(lineRepository.findByPurchaseOrderIdOrderByCreatedAtAsc(line.getPurchaseOrder().getId())).thenReturn(List.of(line));

        PurchaseOrderLine result = service.receive(line.getId(), 8, null);

        assertThat(result.getQuantityReceived()).isEqualTo(8);
        assertThat(result.getStatus()).isEqualTo(PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
    }

    @Test
    void receive_marksFullyReceivedAndClosesOrder_whenExactlyOrdered() {
        PurchaseOrderLine line = openLine(10);
        when(lineRepository.findByIdWithOrder(line.getId())).thenReturn(Optional.of(line));
        when(lineRepository.saveAndFlush(line)).thenReturn(line);
        when(lineRepository.findByPurchaseOrderIdOrderByCreatedAtAsc(line.getPurchaseOrder().getId())).thenReturn(List.of(line));
        when(orderRepository.findByIdWithRequest(line.getPurchaseOrder().getId())).thenReturn(Optional.of(line.getPurchaseOrder()));
        when(orderRepository.saveAndFlush(line.getPurchaseOrder())).thenReturn(line.getPurchaseOrder());

        PurchaseOrderLine result = service.receive(line.getId(), 10, null);

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderLineStatus.FULLY_RECEIVED);
        assertThat(line.getPurchaseOrder().getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
    }

    @Test
    void cancel_requiresReason() {
        PurchaseOrder order = openOrder();

        assertThatThrownBy(() -> service.cancel(order.getId(), " ")).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void cancel_blocksOnceAnyLineReceived() {
        PurchaseOrder order = openOrder();
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(UUID.randomUUID());
        line.setPurchaseOrder(order);
        line.setQuantityOrdered(10);
        line.setQuantityReceived(3);
        when(orderRepository.findByIdWithRequest(order.getId())).thenReturn(Optional.of(order));
        when(lineRepository.findByPurchaseOrderIdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of(line));

        assertThatThrownBy(() -> service.cancel(order.getId(), "No longer needed")).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_succeeds_whenNothingReceivedYet() {
        PurchaseOrder order = openOrder();
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(UUID.randomUUID());
        line.setPurchaseOrder(order);
        line.setQuantityOrdered(10);
        line.setStatus(PurchaseOrderLineStatus.OPEN);
        when(orderRepository.findByIdWithRequest(order.getId())).thenReturn(Optional.of(order));
        when(lineRepository.findByPurchaseOrderIdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of(line));
        when(lineRepository.saveAndFlush(line)).thenReturn(line);
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        PurchaseOrder result = service.cancel(order.getId(), "No longer needed");

        assertThat(result.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        assertThat(line.getStatus()).isEqualTo(PurchaseOrderLineStatus.CANCELLED);
    }

    @Test
    void returnToVendor_rejectsReturningMoreThanReceived() {
        PurchaseOrderLine line = openLine(10);
        line.setQuantityReceived(5);
        when(lineRepository.findByIdWithOrder(line.getId())).thenReturn(Optional.of(line));

        assertThatThrownBy(() -> service.returnToVendor(line.getId(), 6, "Defective units"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void returnToVendor_succeeds() {
        PurchaseOrderLine line = openLine(10);
        line.setQuantityReceived(5);
        when(lineRepository.findByIdWithOrder(line.getId())).thenReturn(Optional.of(line));
        when(lineRepository.saveAndFlush(line)).thenReturn(line);

        PurchaseOrderLine result = service.returnToVendor(line.getId(), 2, "Defective units");

        assertThat(result.getQuantityReturned()).isEqualTo(2);
    }

    private PurchaseOrder openOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(UUID.randomUUID());
        order.setPurchaseRequest(approvedRequest);
        order.setStatus(PurchaseOrderStatus.OPEN);
        return order;
    }

    private PurchaseOrderLine openLine(int quantityOrdered) {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(UUID.randomUUID());
        line.setPurchaseOrder(openOrder());
        line.setQuantityOrdered(quantityOrdered);
        line.setStatus(PurchaseOrderLineStatus.OPEN);
        return line;
    }
}
