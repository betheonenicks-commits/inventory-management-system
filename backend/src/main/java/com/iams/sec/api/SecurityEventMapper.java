package com.iams.sec.api;

import com.iams.sec.api.dto.SecurityEventResponse;
import com.iams.sec.domain.SecurityEventLog;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventMapper {

    public SecurityEventResponse toResponse(SecurityEventLog event) {
        return new SecurityEventResponse(
                event.getId(),
                event.getEventType().name(),
                event.getActorUserId(),
                event.getUsernameAttempted(),
                event.getIpAddress(),
                event.getDetail(),
                event.getCreatedAt()
        );
    }
}
