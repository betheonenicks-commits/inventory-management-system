package com.iams.integration.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.integration.domain.Integration;
import com.iams.integration.domain.IntegrationRepository;
import com.iams.integration.domain.IntegrationType;
import com.iams.integration.domain.SecretReferences;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-SEC-15 / FR-INT-05: register and manage external integrations. The security-critical
 * job here is that a credential is only ever stored as a secrets-manager reference
 * ({@link SecretReferences}) - {@link #create} rejects (400) any inline plaintext secret,
 * in the credential field or smuggled into the config map (AC-SEC-15-X), so nothing but a
 * reference is ever persisted (AC-SEC-15-H). Every integration is disabled until deliberately
 * enabled; create/enable/disable/delete are recorded to the Security &amp; Access Log.
 */
@Service
public class IntegrationService {

    private final IntegrationRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;

    public IntegrationService(IntegrationRepository repository, CurrentUserProvider currentUserProvider,
                              SecurityEventLogger securityEventLogger) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
    }

    @Transactional
    public Integration create(String name, String typeName, String description, String credentialRef,
                              Map<String, String> config) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "Integration name is required");
        }
        if (!IntegrationType.isValid(typeName)) {
            throw ValidationFailedException.singleField("type",
                    "Unknown integration type: " + typeName + ". Valid types: " + validTypes());
        }
        IntegrationType type = IntegrationType.valueOf(typeName);
        if (repository.existsByName(name.trim())) {
            throw new ConflictException("INTEGRATION_NAME_TAKEN",
                    "An integration named '" + name.trim() + "' already exists");
        }

        // AC-SEC-15-X: a credential, where present or required, must be a secrets-manager reference,
        // never an inline secret; and no secret may be smuggled into the (non-secret) config map.
        Map<String, String> settings = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
        SecretReferences.rejectInlineSecretsInConfig(settings);
        if (credentialRef != null && !credentialRef.isBlank()) {
            SecretReferences.requireReference("credentialRef", credentialRef);
        } else if (type.needsCredential()) {
            throw ValidationFailedException.singleField("credentialRef",
                    "A secrets-manager reference is required for a " + type.name() + " integration");
        }

        Integration integration = new Integration();
        integration.setName(name.trim());
        integration.setType(type);
        integration.setDescription(description);
        integration.setCredentialRef(credentialRef == null || credentialRef.isBlank() ? null : credentialRef.trim());
        integration.setConfig(settings);
        integration.setEnabled(false); // FR-INT-05: disabled by default
        integration.setCreatedBy(currentUserProvider.current().id());
        repository.save(integration);

        securityEventLogger.record(SecurityEventType.INTEGRATION_CREATED, currentUserProvider.current().id(),
                null, null, "Integration '" + integration.getName() + "' (" + type.name() + ") registered");
        return integration;
    }

    @Transactional(readOnly = true)
    public List<Integration> list() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Integration get(UUID id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Integration", id));
    }

    @Transactional
    public Integration setEnabled(UUID id, boolean enabled) {
        Integration integration = repository.findById(id).orElseThrow(() -> NotFoundException.of("Integration", id));
        if (integration.isEnabled() == enabled) {
            return integration; // idempotent - no state change, no log
        }
        integration.setEnabled(enabled);
        integration.setUpdatedAt(Instant.now());
        securityEventLogger.record(enabled ? SecurityEventType.INTEGRATION_ENABLED : SecurityEventType.INTEGRATION_DISABLED,
                currentUserProvider.current().id(), null, null,
                "Integration '" + integration.getName() + "' " + (enabled ? "enabled" : "disabled"));
        return integration;
    }

    @Transactional
    public void delete(UUID id) {
        Integration integration = repository.findById(id).orElseThrow(() -> NotFoundException.of("Integration", id));
        repository.delete(integration);
        securityEventLogger.record(SecurityEventType.INTEGRATION_DELETED, currentUserProvider.current().id(),
                null, null, "Integration '" + integration.getName() + "' deleted");
    }

    private static String validTypes() {
        StringBuilder sb = new StringBuilder();
        for (IntegrationType t : IntegrationType.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(t.name());
        }
        return sb.toString();
    }
}
