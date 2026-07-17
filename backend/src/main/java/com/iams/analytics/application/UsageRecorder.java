package com.iams.analytics.application;

import com.iams.analytics.domain.UsageEvent;
import com.iams.analytics.domain.UsageEventRepository;
import com.iams.common.security.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The one write path into usage_event. Never throws: US-ANL-01's second AC
 * says a broken capture pipeline must not affect the business action, so
 * every failure degrades to a WARN log and a lost data point - by the time
 * this runs (afterCompletion) the response is already committed, and this
 * guard keeps even the request thread's cleanup unaffected.
 */
@Service
public class UsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(UsageRecorder.class);

    private final UsageEventRepository repository;

    public UsageRecorder(UsageEventRepository repository) {
        this.repository = repository;
    }

    public void record(CurrentUser user, String module, String action) {
        try {
            List<UsageEvent> events = new ArrayList<>();
            // One row per role held right now (see UsageEvent); a principal with no
            // roles is still a real usage signal, attributed to NONE.
            List<String> roles = user.roles() == null || user.roles().isEmpty()
                    ? List.of("NONE")
                    : List.copyOf(user.roles());
            for (String role : roles) {
                UsageEvent event = new UsageEvent();
                event.setModule(module);
                event.setAction(action);
                event.setRole(role);
                event.setUserId(user.id());
                events.add(event);
            }
            repository.saveAll(events);
        } catch (Exception e) {
            log.warn("Usage capture failed for {}/{} - business action unaffected: {}", module, action, e.getMessage());
        }
    }
}
