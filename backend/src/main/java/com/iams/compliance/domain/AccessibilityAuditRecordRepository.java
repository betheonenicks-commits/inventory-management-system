package com.iams.compliance.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessibilityAuditRecordRepository extends JpaRepository<AccessibilityAuditRecord, UUID> {
}
