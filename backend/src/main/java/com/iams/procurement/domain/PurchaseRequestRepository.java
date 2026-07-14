package com.iams.procurement.domain;

import com.iams.lifecycle.domain.LifecycleRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {

    List<PurchaseRequest> findByStatusOrderByRequestedAtDesc(LifecycleRequestStatus status);

    List<PurchaseRequest> findAllByOrderByRequestedAtDesc();
}
