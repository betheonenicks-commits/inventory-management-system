package com.iams.compliance.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundIntegrationFlagRepository extends JpaRepository<OutboundIntegrationFlag, UUID> {

    List<OutboundIntegrationFlag> findByEnabledTrue();

    List<OutboundIntegrationFlag> findAllByOrderByNameAsc();
}
