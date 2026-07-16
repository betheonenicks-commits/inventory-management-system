package com.iams.report.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {

    List<ReportSchedule> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    Optional<ReportSchedule> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<ReportSchedule> findByStatusAndNextRunAtLessThanEqual(ReportSchedule.Status status, Instant now);
}
