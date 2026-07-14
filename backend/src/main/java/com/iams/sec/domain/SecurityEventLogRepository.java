package com.iams.sec.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventLogRepository extends JpaRepository<SecurityEventLog, UUID>, SecurityEventLogRepositoryCustom {
}
