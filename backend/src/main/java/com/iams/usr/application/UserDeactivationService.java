package com.iams.usr.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.UserStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-USR-08: deactivating a user is blocked while the person they correspond
 * to still holds assigned assets, so offboarding never silently orphans
 * equipment. A user with no linked Person record (personId null) has, by
 * definition, no assets to be blocked by.
 * <p>
 * The blocked-response shape (errorCode, blockingAssets, resolutionActions)
 * matches IAMS_API_Specification_v1.1.md Section 4.5 exactly, since that's a
 * ratified external contract a real client would be coded against - a
 * generic message string wouldn't satisfy it.
 * <p>
 * Session/refresh-token revocation (also named in US-USR-08's happy-path AC)
 * is now fully closed: RefreshTokenService.revokeAll stops the deactivated
 * user's refresh tokens immediately, and DeactivatedUserRegistry stops their
 * already-issued stateless access token at the very next request (the JWT
 * filter consults the registry), rather than letting it live to its natural
 * expiry.
 */
@Service
public class UserDeactivationService {

    private final AppUserRepository userRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;
    private final RefreshTokenService refreshTokenService;
    private final DeactivatedUserRegistry deactivatedUserRegistry;

    public UserDeactivationService(AppUserRepository userRepository, AssetRepository assetRepository,
                                    CurrentUserProvider currentUserProvider, SecurityEventLogger securityEventLogger,
                                    RefreshTokenService refreshTokenService,
                                    DeactivatedUserRegistry deactivatedUserRegistry) {
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
        this.refreshTokenService = refreshTokenService;
        this.deactivatedUserRegistry = deactivatedUserRegistry;
    }

    @Transactional
    public AppUser deactivate(UUID userId, long expectedVersion) {
        AppUser user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        if (user.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, user.getVersion(), user);
        }

        if (user.getPersonId() != null) {
            List<Asset> blockingAssets = assetRepository.findByAssignedToPersonId(user.getPersonId());
            if (!blockingAssets.isEmpty()) {
                throw blockedByAssignedAssets(blockingAssets);
            }
        }

        user.setStatus(UserStatus.DEACTIVATED);
        UUID actorId = currentUserProvider.current().id();
        user.setUpdatedBy(actorId);

        try {
            AppUser saved = userRepository.saveAndFlush(user);
            refreshTokenService.revokeAll(userId);
            // US-USR-08: refuse this user's still-unexpired access tokens from the next request on.
            deactivatedUserRegistry.markDeactivated(userId);
            securityEventLogger.record(SecurityEventType.USER_DEACTIVATED, actorId, user.getUsername(), null, null);
            return saved;
        } catch (OptimisticLockingFailureException e) {
            AppUser current = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }

    private ConflictException blockedByAssignedAssets(List<Asset> blockingAssets) {
        List<Map<String, Object>> blockingAssetPayload = blockingAssets.stream().map(asset -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("assetId", asset.getId());
            entry.put("assetNumber", asset.getAssetNumber());
            entry.put("name", asset.getName());
            // Best-effort: the asset record doesn't track an assignment-specific
            // timestamp separately from its general updatedAt (no per-assignment
            // history query exists at this layer yet) - updatedAt is usually the
            // assignment moment in practice but isn't guaranteed to be.
            entry.put("assignedSince", asset.getUpdatedAt());
            return entry;
        }).toList();

        Map<String, Object> extraProperties = new LinkedHashMap<>();
        extraProperties.put("blockingAssets", blockingAssetPayload);
        extraProperties.put("resolutionActions", List.of(
                "POST /api/v1/assets/{assetId}/assign — reassign to another holder",
                "POST /api/v1/assets/{assetId}/return-to-inventory — return, awaiting reissue"));

        return new ConflictException(
                "USER_HAS_OUTSTANDING_ASSIGNMENTS",
                "Cannot deactivate user with outstanding asset assignments",
                blockingAssets.size() + " assets are currently assigned to this user and must be reassigned "
                        + "or returned to inventory before deactivation.",
                extraProperties);
    }
}
