package com.iams.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.usr.application.OrgScopeGuard;
import com.iams.usr.application.UserScopeResolver;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetHierarchyServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetHistoryRecorder historyRecorder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserScopeResolver scopeResolver;

    @Mock
    private OrgNodeRepository orgNodeRepository;

    @Mock
    private SecurityEventLogger securityEventLogger;

    private AssetHierarchyService service;

    @BeforeEach
    void setUp() {
        OrgScopeGuard scopeGuard = new OrgScopeGuard(currentUserProvider, scopeResolver, orgNodeRepository, securityEventLogger);
        service = new AssetHierarchyService(assetRepository, historyRecorder, currentUserProvider, scopeGuard);
    }

    private Asset asset(String number) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber(number);
        return asset;
    }

    private void stubCurrentUser() {
        stubUser(new CurrentUser(UUID.randomUUID(), "tester", Set.of("SUPER_ADMIN")));
    }

    // OrgScopeGuard resolves scope via currentOrEmpty() and logs a denial via current(); the
    // service stamps updatedBy via current() on reparent. Stub both (leniently, since any one
    // test exercises only some of these paths) to the same actor.
    private void stubUser(CurrentUser user) {
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
    void linkChild_succeeds_andRecordsHistoryOnChild() {
        Asset parent = asset("AST-2026-000001");
        Asset child = asset("AST-2026-000002");
        stubCurrentUser();
        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(assetRepository.findByIdWithAssociations(child.getId())).thenReturn(Optional.of(child));
        when(assetRepository.existsByParentAssetId(child.getId())).thenReturn(false);
        when(assetRepository.saveAndFlush(child)).thenReturn(child);

        Asset result = service.linkChild(parent.getId(), child.getId());

        assertThat(result.getParentAsset()).isSameAs(parent);
        verify(historyRecorder).record(child, AssetHistoryEventType.FIELD_UPDATE, "parentAssetId", null, parent.getAssetNumber());
    }

    @Test
    void linkChild_rejectsSelfLink() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.linkChild(id, id))
                .isInstanceOf(ValidationFailedException.class);

        verify(assetRepository, never()).findById(any());
    }

    @Test
    void linkChild_rejectsWhenParentNotFound() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        when(assetRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.linkChild(parentId, childId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void linkChild_rejectsWhenChildAlreadyHasParent() {
        Asset parent = asset("AST-2026-000001");
        Asset otherParent = asset("AST-2026-000003");
        Asset child = asset("AST-2026-000002");
        child.setParentAsset(otherParent);
        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(assetRepository.findByIdWithAssociations(child.getId())).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.linkChild(parent.getId(), child.getId()))
                .isInstanceOf(ConflictException.class);

        verify(historyRecorder, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void linkChild_rejectsWhenParentIsItselfAChild() {
        Asset grandparent = asset("AST-2026-000009");
        Asset parent = asset("AST-2026-000001");
        parent.setParentAsset(grandparent);
        Asset child = asset("AST-2026-000002");
        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(assetRepository.findByIdWithAssociations(child.getId())).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.linkChild(parent.getId(), child.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void linkChild_rejectsWhenChildAlreadyHasItsOwnChildren() {
        Asset parent = asset("AST-2026-000001");
        Asset child = asset("AST-2026-000002");
        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(assetRepository.findByIdWithAssociations(child.getId())).thenReturn(Optional.of(child));
        when(assetRepository.existsByParentAssetId(child.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.linkChild(parent.getId(), child.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unlinkChild_succeeds_andRecordsHistoryOnChild() {
        Asset parent = asset("AST-2026-000001");
        Asset child = asset("AST-2026-000002");
        child.setParentAsset(parent);
        stubCurrentUser();
        when(assetRepository.findById(child.getId())).thenReturn(Optional.of(child));
        when(assetRepository.saveAndFlush(child)).thenReturn(child);

        service.unlinkChild(parent.getId(), child.getId());

        assertThat(child.getParentAsset()).isNull();
        verify(historyRecorder).record(child, AssetHistoryEventType.FIELD_UPDATE, "parentAssetId", parent.getAssetNumber(), null);
    }

    @Test
    void unlinkChild_rejectsWhenChildNotLinkedToThatParent() {
        Asset child = asset("AST-2026-000002");
        UUID unrelatedParentId = UUID.randomUUID();
        when(assetRepository.findById(child.getId())).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service.unlinkChild(unrelatedParentId, child.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listChildren_returnsRepositoryResult() {
        stubCurrentUser();
        Asset parent = asset("AST-2026-000001");
        Asset child = asset("AST-2026-000002");
        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(assetRepository.findByParentAssetIdWithAssociationsOrderByCreatedAtAsc(parent.getId())).thenReturn(List.of(child));

        List<Asset> result = service.listChildren(parent.getId());

        assertThat(result).containsExactly(child);
    }

    @Test
    void listChildren_rejectsWhenParentNotFound() {
        UUID parentId = UUID.randomUUID();
        when(assetRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listChildren(parentId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listChildren_blocked_whenParentOutsideRequesterScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        stubUser(new CurrentUser(actorId, "deptHead", Set.of("DEPARTMENT_HEAD")));
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        nodeAt(scopeNodeId, "/" + scopeNodeId + "/");

        OrgNode otherNode = nodeAt(UUID.randomUUID(), "/" + UUID.randomUUID() + "/");
        Asset parent = asset("AST-2026-000001");
        parent.setOrgNode(otherNode);
        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.listChildren(parent.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void listChildren_filtersOutChildrenOutsideRequesterScope() {
        UUID actorId = UUID.randomUUID();
        UUID scopeNodeId = UUID.randomUUID();
        stubUser(new CurrentUser(actorId, "deptHead", Set.of("DEPARTMENT_HEAD")));
        when(scopeResolver.resolveScopeOrgNodeId(actorId)).thenReturn(Optional.of(scopeNodeId));
        String scopePath = "/" + scopeNodeId + "/";
        nodeAt(scopeNodeId, scopePath);

        OrgNode inScopeNode = nodeAt(scopeNodeId, scopePath);
        OrgNode outOfScopeNode = nodeAt(UUID.randomUUID(), "/" + UUID.randomUUID() + "/");

        Asset parent = asset("AST-2026-000001");
        parent.setOrgNode(inScopeNode);
        Asset inScopeChild = asset("AST-2026-000002");
        inScopeChild.setOrgNode(inScopeNode);
        Asset outOfScopeChild = asset("AST-2026-000003");
        outOfScopeChild.setOrgNode(outOfScopeNode);

        when(assetRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(assetRepository.findByParentAssetIdWithAssociationsOrderByCreatedAtAsc(parent.getId()))
                .thenReturn(List.of(inScopeChild, outOfScopeChild));
        when(orgNodeRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<UUID> ids = invocation.getArgument(0);
            List<OrgNode> result = new java.util.ArrayList<>();
            for (UUID id : ids) {
                if (id.equals(scopeNodeId)) {
                    result.add(inScopeNode);
                } else if (id.equals(outOfScopeNode.getId())) {
                    result.add(outOfScopeNode);
                }
            }
            return result;
        });

        List<Asset> result = service.listChildren(parent.getId());

        assertThat(result).containsExactly(inScopeChild);
    }
}
