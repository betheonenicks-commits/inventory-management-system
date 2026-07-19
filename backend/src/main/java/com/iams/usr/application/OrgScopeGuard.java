package com.iams.usr.application;

import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * FR-USR-04 org-scope enforcement, factored out of AssetQueryService and
 * PersonService (which had each grown an identical copy of this check) so
 * that a third, fourth, ... consumer gets the same behavior by construction
 * rather than by remembering to copy-paste it correctly again.
 * <p>
 * Descendant-node matching (EPIC-ORG's hierarchy, added 2026-07-13): a
 * scoped actor sees their own node AND everything under it, via
 * {@code OrgNode.path} prefix matching - "Campus North" scope also covers
 * every Building/Floor/Room beneath it, not just an exact-id match. See
 * {@link UserScopeResolver} for why the scope itself is still resolved fresh
 * per request rather than cached.
 */
@Slf4j
@Component
public class OrgScopeGuard {

    private final CurrentUserProvider currentUserProvider;
    private final UserScopeResolver scopeResolver;
    private final OrgNodeRepository orgNodeRepository;
    private final SecurityEventLogger securityEventLogger;

    public OrgScopeGuard(CurrentUserProvider currentUserProvider, UserScopeResolver scopeResolver,
                          OrgNodeRepository orgNodeRepository, SecurityEventLogger securityEventLogger) {
        this.currentUserProvider = currentUserProvider;
        this.scopeResolver = scopeResolver;
        this.orgNodeRepository = orgNodeRepository;
        this.securityEventLogger = securityEventLogger;
    }

    /** The acting user's scope org-node id, or null if they're unrestricted. */
    public UUID currentScopeOrgNodeId() {
        return scopeResolver.resolveScopeOrgNodeId(currentUserProvider.current().id()).orElse(null);
    }

    /**
     * The acting user's scope-node path, for callers (AssetRepositoryImpl) that need to
     * build their own "starts with this prefix" query rather than checking one entity at
     * a time. Null means unrestricted.
     */
    public String currentScopePathPrefix() {
        UUID scopeId = currentScopeOrgNodeId();
        return scopeId == null ? null : orgNodeRepository.findById(scopeId).map(OrgNode::getPath).orElse(null);
    }

    /**
     * Throws AccessDeniedException (403) if the caller is scoped and entityOrgNodeId is
     * not the scope node itself or one of its descendants - including when
     * entityOrgNodeId is null (an unscoped entity is not visible to a scoped actor;
     * ambiguity is resolved conservatively, not permissively).
     */
    public void requireWithinScope(UUID entityOrgNodeId, String entityType, Object entityId) {
        String scopePath = currentScopePathPrefix();
        if (scopePath == null) {
            return;
        }
        String entityPath = entityOrgNodeId != null
                ? orgNodeRepository.findById(entityOrgNodeId).map(OrgNode::getPath).orElse(null)
                : null;
        if (entityPath == null || !entityPath.startsWith(scopePath)) {
            // AC-USR-04-X: refused, and recorded to the Security & Access Log (US-SEC-04),
            // not just the application log - the SLF4J line stays too, for local dev
            // visibility without a DB round-trip.
            log.warn("Permission denied: {} {} is outside requester's org scope path {}", entityType, entityId, scopePath);
            securityEventLogger.record(SecurityEventType.PERMISSION_DENIED, currentUserProvider.current().id(), null, null,
                    entityType + " " + entityId + " is outside requester's org scope");
            throw new AccessDeniedException("This " + entityType + " is outside your organizational scope");
        }
    }

    /**
     * Refuse (403, recorded to the Security &amp; Access Log) when a scoped caller has no
     * visibility of an entity whose in-scope determination the caller computes itself -
     * used for entities whose footprint is a SET of locations rather than one org node
     * (an audit spanning several expected-asset locations, with no single scope node of
     * its own). No-op for an unrestricted caller or when {@code inScope} is true. Mirrors
     * {@link #requireWithinScope}'s refusal exactly, so both paths log and 403 identically.
     */
    public void requireInScope(boolean inScope, String entityType, Object entityId) {
        if (currentScopePathPrefix() == null || inScope) {
            return;
        }
        log.warn("Permission denied: {} {} is outside requester's org scope", entityType, entityId);
        securityEventLogger.record(SecurityEventType.PERMISSION_DENIED, currentUserProvider.current().id(), null, null,
                entityType + " " + entityId + " is outside requester's org scope");
        throw new AccessDeniedException("This " + entityType + " is outside your organizational scope");
    }

    /** In-scope subset of a list of (entity, orgNodeId) pairs - for list endpoints, not single-record detail fetches. */
    public <T> List<T> filterToScope(List<T> entities, Function<T, UUID> orgNodeIdExtractor) {
        String scopePath = currentScopePathPrefix();
        if (scopePath == null) {
            return entities;
        }
        Set<UUID> orgNodeIds = entities.stream().map(orgNodeIdExtractor).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> pathsById = orgNodeRepository.findAllById(orgNodeIds).stream()
                .collect(Collectors.toMap(OrgNode::getId, OrgNode::getPath));
        return entities.stream()
                .filter(e -> {
                    UUID orgNodeId = orgNodeIdExtractor.apply(e);
                    String entityPath = orgNodeId != null ? pathsById.get(orgNodeId) : null;
                    return entityPath != null && entityPath.startsWith(scopePath);
                })
                .toList();
    }
}
