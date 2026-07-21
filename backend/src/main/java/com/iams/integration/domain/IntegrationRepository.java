package com.iams.integration.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, UUID> {

    boolean existsByName(String name);

    List<Integration> findAllByOrderByCreatedAtDesc();
}
