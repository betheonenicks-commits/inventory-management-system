package com.iams.procurement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.application.ApprovalRoutingService;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.procurement.domain.PurchaseRequest;
import com.iams.procurement.domain.PurchaseRequestRepository;
import com.iams.usr.domain.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class PurchaseRequestServiceTest {

    @Mock private PurchaseRequestRepository requestRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private ApprovalRoutingService routingService;
    @Mock private CurrentUserProvider currentUserProvider;

    private PurchaseRequestService service;
    private UUID actorId;
    private UUID approverId;

    @BeforeEach
    void setUp() {
        service = new PurchaseRequestService(requestRepository, appUserRepository, routingService, currentUserProvider);
        actorId = UUID.randomUUID();
        approverId = UUID.randomUUID();
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(actorId, "manager1", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_rejectsBlankJustification() {
        assertThatThrownBy(() -> service.create(new PurchaseRequestCreateCommand("Laptop", " ", BigDecimal.TEN, null, approverId)))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnknownApprover() {
        when(appUserRepository.existsById(approverId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new PurchaseRequestCreateCommand("Laptop", "Needed for new hire", BigDecimal.TEN, null, approverId)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_succeeds() {
        when(appUserRepository.existsById(approverId)).thenReturn(true);
        when(requestRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequest result = service.create(new PurchaseRequestCreateCommand("Laptop", "Needed for new hire", new BigDecimal("999.00"), "Dell", approverId));

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.PENDING);
        assertThat(result.getRequestedBy()).isEqualTo(actorId);
    }

    @Test
    void approve_rejectsAnyoneOtherThanRoutedApprover() {
        PurchaseRequest request = pendingRequest();
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approve_succeeds_whenActedByRoutedApprover() {
        PurchaseRequest request = pendingRequest();
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(approverId, "depthead", Set.of("DEPARTMENT_HEAD")));
        when(routingService.resolveEffectiveApprover(approverId)).thenReturn(approverId);
        when(requestRepository.saveAndFlush(request)).thenReturn(request);

        PurchaseRequest result = service.approve(request.getId());

        assertThat(result.getStatus()).isEqualTo(LifecycleRequestStatus.APPROVED);
        assertThat(result.getDecidedBy()).isEqualTo(approverId);
    }

    @Test
    void approve_rejectsWhenNotPending() {
        PurchaseRequest request = pendingRequest();
        request.setStatus(LifecycleRequestStatus.REJECTED);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(request.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void reject_requiresReason() {
        PurchaseRequest request = pendingRequest();

        assertThatThrownBy(() -> service.reject(request.getId(), " ")).isInstanceOf(ValidationFailedException.class);
    }

    private PurchaseRequest pendingRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setId(UUID.randomUUID());
        request.setItemDescription("Laptop");
        request.setJustification("Needed for new hire");
        request.setStatus(LifecycleRequestStatus.PENDING);
        request.setNominalApproverId(approverId);
        request.setRequestedBy(actorId);
        request.setRequestedAt(Instant.now());
        return request;
    }
}
