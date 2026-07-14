package com.iams.sec.application;

import com.iams.sec.domain.SecurityEventLog;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-SEC-11: search and filter the Security & Access Log. */
@Service
public class SecurityEventLogService {

    private final SecurityEventLogRepository repository;

    public SecurityEventLogService(SecurityEventLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventLog> search(UUID actorUserId, SecurityEventType eventType,
                                          Instant from, Instant to, Pageable pageable) {
        return repository.search(actorUserId, eventType, from, to, pageable);
    }
}
