package com.iams.platform.application;

import com.iams.platform.api.dto.SystemHealthResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

/**
 * US-USR-05 (AC-USR-05-H): a real, permission-gated view of the app's own
 * health for the System Operator role - the one business-data-free surface
 * this story names that's actually buildable today (unlike backup/LDAP
 * configuration, which have no underlying subsystem to gate access to yet).
 * Reuses Spring Boot's own health aggregation (DataSource, disk space, etc.)
 * rather than hand-rolling checks - this calls the HealthEndpoint bean
 * directly, not the actuator HTTP endpoint, so it's unaffected by
 * management.endpoint.health.show-details (that setting only governs what
 * the anonymous /actuator/health path exposes).
 */
@Service
public class SystemHealthService {

    private final HealthEndpoint healthEndpoint;

    public SystemHealthService(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    public SystemHealthResponse check() {
        HealthComponent health = healthEndpoint.health();
        Map<String, String> components = new TreeMap<>();
        if (health instanceof CompositeHealth composite) {
            composite.getComponents().forEach((name, component) -> components.put(name, component.getStatus().getCode()));
        }
        return new SystemHealthResponse(health.getStatus().getCode(), new LinkedHashMap<>(components), Instant.now());
    }
}
