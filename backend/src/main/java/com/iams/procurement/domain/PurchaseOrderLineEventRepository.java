package com.iams.procurement.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderLineEventRepository extends JpaRepository<PurchaseOrderLineEvent, UUID> {

    List<PurchaseOrderLineEvent> findByLineIdOrderByCreatedAtAsc(UUID lineId);
}
