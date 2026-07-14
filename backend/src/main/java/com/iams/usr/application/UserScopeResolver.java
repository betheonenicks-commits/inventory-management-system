package com.iams.usr.application;

import com.iams.usr.domain.AppUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-USR-04: resolves the acting user's current org-scope node, fresh from
 * the database on every call - never cached in the JWT or anywhere else.
 * That's deliberate, not an oversight: US-USR-04's own exception AC requires
 * a scope narrowed mid-session to be enforced on the user's very next
 * request, which a claim baked into a still-valid access token could not do
 * without a token-revocation mechanism this system doesn't have yet.
 * <p>
 * An empty result means unrestricted (typically Super Administrator, or any
 * role provisioned without an org-scope node) - callers must treat that as
 * "no filter," not "match nothing."
 * <p>
 * This resolver returns only the scope node's own id; it does not do
 * descendant matching itself. Since EPIC-ORG's hierarchy (2026-07-13),
 * OrgScopeGuard is what turns that id into a "this node or any descendant"
 * check, via OrgNode.path prefix matching - this class deliberately stays a
 * thin id lookup so it doesn't duplicate that logic.
 */
@Service
public class UserScopeResolver {

    private final AppUserRepository userRepository;

    public UserScopeResolver(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> resolveScopeOrgNodeId(UUID userId) {
        // Optional.map already collapses a null-returning mapper to Optional.empty(),
        // so "user not found" and "user has no scope node" both correctly read as
        // "unrestricted" to callers - which is the right behavior for both cases.
        return userRepository.findById(userId)
                .map(user -> user.getOrgScopeNode() != null ? user.getOrgScopeNode().getId() : null);
    }
}
