package com.iams.report.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdHocReportRepository extends JpaRepository<AdHocReport, UUID> {

    List<AdHocReport> findByUserIdOrderByNameAsc(UUID userId);

    Optional<AdHocReport> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
