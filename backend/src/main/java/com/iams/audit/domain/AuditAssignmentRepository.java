package com.iams.audit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditAssignmentRepository extends JpaRepository<AuditAssignment, UUID> {

    List<AuditAssignment> findByAuditIdOrderByCreatedAtAsc(UUID auditId);

    Optional<AuditAssignment> findByAuditIdAndAuditorUserIdAndActiveTrue(UUID auditId, UUID auditorUserId);
}
