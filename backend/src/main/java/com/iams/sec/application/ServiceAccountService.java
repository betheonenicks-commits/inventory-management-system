package com.iams.sec.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.common.security.ServiceAccountAuthenticator;
import com.iams.common.security.ServiceAccountPrincipal;
import com.iams.sec.domain.IntegrationScope;
import com.iams.sec.domain.SecurityEventType;
import com.iams.sec.domain.ServiceAccount;
import com.iams.sec.domain.ServiceAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-SEC-14 (AC-SEC-14-H) + US-SEC-15: issue, authenticate, list, and revoke scoped
 * integration service accounts. The credential is a random API key; only its SHA-256
 * hash is persisted (US-SEC-15 - the raw key is returned exactly once, at creation),
 * exactly as {@code RefreshTokenService} treats refresh tokens. Implements the
 * {@link ServiceAccountAuthenticator} port the auth filter consults.
 */
@Service
public class ServiceAccountService implements ServiceAccountAuthenticator {

    private static final int KEY_BYTES = 32;
    private static final String KEY_PREFIX = "iamssvc_";
    /** last_used_at is refreshed at most this often, to keep the auth hot path from writing on every request. */
    private static final long LAST_USED_THROTTLE_SECONDS = 60;

    private final ServiceAccountRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;
    private final SecureRandom random = new SecureRandom();

    public ServiceAccountService(ServiceAccountRepository repository, CurrentUserProvider currentUserProvider,
                                  SecurityEventLogger securityEventLogger) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
    }

    /** The one and only time the raw key is exposed - it is never recoverable afterwards. */
    public record Issued(ServiceAccount account, String rawApiKey) {
    }

    @Transactional
    public Issued create(String name, String description, Set<String> scopes) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "Service account name is required");
        }
        if (repository.existsByName(name.trim())) {
            throw new ConflictException("SERVICE_ACCOUNT_NAME_TAKEN",
                    "A service account named '" + name.trim() + "' already exists");
        }
        Set<String> requested = scopes == null ? Set.of() : scopes;
        if (requested.isEmpty()) {
            throw ValidationFailedException.singleField("scopes", "At least one scope is required");
        }
        for (String scope : requested) {
            if (!IntegrationScope.isValid(scope)) {
                throw ValidationFailedException.singleField("scopes",
                        "Unknown scope: " + scope + ". Valid scopes: " + IntegrationScope.names());
            }
        }

        String rawKey = KEY_PREFIX + generateSecret();
        ServiceAccount account = new ServiceAccount();
        account.setName(name.trim());
        account.setDescription(description);
        account.setApiKeyHash(hash(rawKey));
        account.setApiKeyPrefix(rawKey.substring(0, KEY_PREFIX.length() + 4)); // e.g. "iamssvc_ab12"
        account.setScopes(new HashSet<>(requested));
        account.setActive(true);
        account.setCreatedBy(currentUserProvider.current().id());
        repository.save(account);

        securityEventLogger.record(SecurityEventType.SERVICE_ACCOUNT_CREATED, currentUserProvider.current().id(),
                null, null, "Service account '" + account.getName() + "' scopes=" + account.getScopes());
        return new Issued(account, rawKey);
    }

    /** US-SEC-14 auth hot path: hash the presented key and resolve to a principal. */
    @Override
    @Transactional
    public Optional<ServiceAccountPrincipal> authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByApiKeyHash(hash(rawApiKey))
                .filter(ServiceAccount::isActive)
                .map(account -> {
                    Instant now = Instant.now();
                    if (account.getLastUsedAt() == null
                            || account.getLastUsedAt().isBefore(now.minusSeconds(LAST_USED_THROTTLE_SECONDS))) {
                        account.setLastUsedAt(now); // throttled, so this isn't a write on every request
                    }
                    return new ServiceAccountPrincipal(account.getId(), account.getName(), Set.copyOf(account.getScopes()));
                });
    }

    @Transactional(readOnly = true)
    public List<ServiceAccount> list() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void revoke(UUID id) {
        ServiceAccount account = repository.findById(id).orElseThrow(() -> NotFoundException.of("ServiceAccount", id));
        if (!account.isActive()) {
            return; // idempotent - already revoked
        }
        account.setActive(false);
        securityEventLogger.record(SecurityEventType.SERVICE_ACCOUNT_REVOKED, currentUserProvider.current().id(),
                null, null, "Service account '" + account.getName() + "' revoked");
    }

    private String generateSecret() {
        byte[] bytes = new byte[KEY_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
