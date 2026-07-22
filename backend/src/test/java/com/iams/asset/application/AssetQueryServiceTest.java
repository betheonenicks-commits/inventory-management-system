package com.iams.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.application.UserScopeResolver;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Covers FR-USR-04 org-scope enforcement (US-USR-04), which AssetQueryService
 * didn't have any test coverage for before this landed - there was no
 * AssetQueryServiceTest at all until now.
 * <p>
 * Uses a real OrgScopeGuard (backed by mocked CurrentUserProvider/
 * UserScopeResolver/OrgNodeRepository) rather than mocking the guard itself,
 * so these tests still exercise the actual scope-check logic, not just that
 * some method got called on a stub. Scope is path-prefix based (EPIC-ORG's
 * hierarchy, 2026-07-13): an org node "in scope" is one whose path starts
 * with the scope node's own path.
 */
@ExtendWith(MockitoExtension.class)
class AssetQueryServiceTest {

    @Mock private AssetRepository assetRepository;
    @Mock private AssetHistoryEventRepository historyRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private UserScopeResolver scopeResolver;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private SecurityEventLogger securityEventLogger;

    private AssetQueryService service;

    @BeforeEach
    void setUp() {
        OrgScopeGuard scopeGuard = new OrgScopeGuard(currentUserProvider, scopeResolver, orgNodeRepository, securityEventLogger);
        service = new AssetQueryService(assetRepository, historyRepository, scopeGuard, orgNodeRepository);
    }

    // The real OrgScopeGuard reads the actor via currentOrEmpty() for scope resolution and via
    // current() only on the denial-log path, so both are stubbed (leniently - a given test hits
    // only one path) to the same user.
    private void stubActor(UUID actorId) {
        CurrentUser user = new CurrentUser(actorId, "actor", Set.of("AUDITOR"));
        lenient().when(currentUserProvider.current()).thenReturn(user);
        lenient().when(currentUserProvider.currentOrEmpty()).thenReturn(Optional.of(user));
    }

    private OrgNode nodeAt(UUID id, String path) {
        OrgNode node = new OrgNode();
        node.setId(id);
        node.setPath(path);
        lenient().when(orgNodeRepository.findById(id)).thenReturn(Optional.of(node));
        return node;
    }

    @Test
    void get_succeeds_whenActorIsUnscoped() {
        UUID actorId = UUID.randomUUID();
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.empty());

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        assertThat(service.get(asset.getId())).isSameAs(asset);
    }

    @Test
    void get_succeeds_whenAssetWithinScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, "/" + scopeNodeId + "/");

        OrgNode node = nodeAt(scopeNodeId, "/" + scopeNodeId + "/");
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setOrgNode(node);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        assertThat(service.get(asset.getId())).isSameAs(asset);
    }

    @Test
    void get_succeeds_whenAssetIsDescendantOfScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        UUID childNodeId = UUID.randomUUID();
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, "/" + scopeNodeId + "/");
        OrgNode child = nodeAt(childNodeId, "/" + scopeNodeId + "/" + childNodeId + "/");

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setOrgNode(child);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        assertThat(service.get(asset.getId())).isSameAs(asset);
    }

    @Test
    void get_blocked_whenAssetOutsideScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, "/" + scopeNodeId + "/");

        OrgNode otherNode = nodeAt(UUID.randomUUID(), "/" + UUID.randomUUID() + "/");
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setOrgNode(otherNode);
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.get(asset.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void get_blocked_whenAssetHasNoOrgNodeButActorIsScoped() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, "/" + scopeNodeId + "/");

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        when(assetRepository.findByIdWithAssociations(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.get(asset.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void get_rejectsUnknownAsset() {
        UUID id = UUID.randomUUID();
        when(assetRepository.findByIdWithAssociations(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_passesResolvedScopePathToRepositorySearch() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        String scopePath = "/" + scopeNodeId + "/";
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, scopePath);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Asset> emptyPage = new PageImpl<>(java.util.List.of());
        when(assetRepository.search(isNull(), isNull(), isNull(), isNull(), eq(scopePath), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        service.list(null, null, null, null, null, null, null, null, pageable);

        verify(assetRepository).search(isNull(), isNull(), isNull(), isNull(), eq(scopePath), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void list_passesNullScope_whenActorUnscoped() {
        UUID actorId = UUID.randomUUID();
        stubActor(actorId);
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);
        Page<Asset> emptyPage = new PageImpl<>(java.util.List.of());
        when(assetRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        service.list(null, null, null, null, null, null, null, null, pageable);

        verify(assetRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void list_rejectsUnsupportedSortField() {
        // AC-SRC-03-X: an arbitrary property would otherwise become a Criteria
        // path error deep inside Hibernate - rejected up front as a 400 instead.
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("customAttributes"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.list(null, null, null, null, null, null, null, null, pageable))
                .isInstanceOf(com.iams.common.exception.ValidationFailedException.class);
        org.mockito.Mockito.verifyNoInteractions(assetRepository);
    }

    @Test
    void list_rejectsInvertedPurchaseDateRange() {
        Pageable pageable = PageRequest.of(0, 20);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.list(null, null, null, null,
                        java.time.LocalDate.of(2026, 7, 10), java.time.LocalDate.of(2026, 7, 1), null, null, pageable))
                .isInstanceOf(com.iams.common.exception.ValidationFailedException.class);
    }
}
