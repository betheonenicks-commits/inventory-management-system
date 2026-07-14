package com.iams.sec.application;

import com.iams.sec.domain.SecurityEventLog;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.sec.domain.SecurityEventType;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place every US-SEC-04 log row gets written, mirroring
 * AssetHistoryRecorder's shape (asset module's equivalent single-writer).
 * actorUserId is an explicit nullable parameter, not read from
 * CurrentUserProvider, because the most important case (a failed login) has
 * no authenticated actor to read - there is no SecurityContext yet at that
 * point in the request.
 * <p>
 * REQUIRES_NEW is deliberate, not incidental: many callers (OrgScopeGuard's
 * PERMISSION_DENIED, RefreshTokenService's REFRESH_TOKEN_REUSE_DETECTED/
 * SESSION_EXPIRED) record an event and then throw, inside an already-open
 * @Transactional method. Without its own independent transaction, this
 * write joins the caller's transaction (Spring's default REQUIRED
 * propagation) and gets rolled back along with everything else the moment
 * that exception propagates - silently losing exactly the denial/failure
 * events the log exists to capture. Found via live click-testing: a
 * REFRESH_TOKEN_REUSE_DETECTED event that appeared to log successfully
 * (no error at the call site) simply wasn't in the database afterward.
 */
@Component
public class SecurityEventLogger {

    private final SecurityEventLogRepository repository;

    public SecurityEventLogger(SecurityEventLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SecurityEventType eventType, UUID actorUserId, String usernameAttempted,
                        String ipAddress, String detail) {
        SecurityEventLog event = new SecurityEventLog();
        event.setEventType(eventType);
        event.setActorUserId(actorUserId);
        event.setUsernameAttempted(usernameAttempted);
        event.setIpAddress(ipAddress);
        event.setDetail(detail);
        repository.save(event);
    }
}
