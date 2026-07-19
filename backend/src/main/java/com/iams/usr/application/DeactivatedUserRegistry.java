package com.iams.usr.application;

import com.iams.common.security.AccessRevocationCheck;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.UserStatus;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * US-USR-08: the access-token revocation registry. Deactivation is terminal in
 * this system (there is no reactivation flow - {@link UserStatus} only moves
 * ACTIVE -> DEACTIVATED), so the set of revoked users only ever grows and never
 * needs an eviction/removal path.
 * <p>
 * An in-memory {@link Set} keeps the per-request check O(1) with no database hit
 * on the authentication hot path - the reason access tokens were made stateless
 * JWTs in the first place. It is seeded at startup from every already-deactivated
 * user so the guarantee survives a restart (an access token issued just before a
 * restart is still within its lifetime afterwards), and updated the moment a new
 * deactivation commits.
 * <p>
 * Known limitation, stated plainly: this registry is per-instance. This is a
 * single-instance, deployment-local application (the same assumption every prior
 * epic documents); a horizontally-scaled deployment would need a shared store
 * (or a short access-token lifetime accepted as the revocation window).
 */
@Component
@Order(1) // after BootstrapUserSeeder(@Order 0) so the seeded admin exists; harmless regardless
public class DeactivatedUserRegistry implements AccessRevocationCheck, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeactivatedUserRegistry.class);

    private final AppUserRepository userRepository;
    private final Set<UUID> revoked = ConcurrentHashMap.newKeySet();

    public DeactivatedUserRegistry(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        var ids = userRepository.findIdsByStatus(UserStatus.DEACTIVATED);
        revoked.addAll(ids);
        log.info("Access-revocation registry seeded with {} deactivated user(s)", ids.size());
    }

    /** Called when a deactivation commits, so the user's live access tokens stop working at once. */
    public void markDeactivated(UUID userId) {
        revoked.add(userId);
    }

    @Override
    public boolean isRevoked(UUID userId) {
        return userId != null && revoked.contains(userId);
    }
}
