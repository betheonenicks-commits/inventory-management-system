package com.iams.org.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgLevel;
import com.iams.org.domain.OrgLevelRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.PersonRepository;
import com.iams.usr.domain.AppUserRepository;
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
class OrgHierarchyServiceTest {

    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private OrgLevelRepository orgLevelRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private PersonRepository personRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private OrgHierarchyService service;

    @BeforeEach
    void setUp() {
        service = new OrgHierarchyService(orgNodeRepository, orgLevelRepository, assetRepository,
                personRepository, appUserRepository, currentUserProvider);
        lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "super", Set.of("SUPER_ADMIN")));
    }

    private OrgLevel level(String code, int rank) {
        OrgLevel level = new OrgLevel();
        level.setId(UUID.randomUUID());
        level.setCode(code);
        level.setName(code);
        level.setRank(rank);
        level.setVersion(0L);
        return level;
    }

    @Test
    void create_root_succeeds() {
        OrgLevel campus = level("CAMPUS", 0);
        when(orgNodeRepository.existsByCode("MAIN")).thenReturn(false);
        when(orgLevelRepository.findById(campus.getId())).thenReturn(Optional.of(campus));
        when(orgNodeRepository.save(any(OrgNode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orgNodeRepository.findByIdWithLevel(any(UUID.class))).thenAnswer(inv -> {
            OrgNode n = new OrgNode();
            n.setId(inv.getArgument(0));
            n.setLevel(campus);
            n.setPath("/" + inv.getArgument(0) + "/");
            return Optional.of(n);
        });

        OrgNode result = service.create("Main Campus", "MAIN", null, campus.getId(), null);

        assertThat(result.getPath()).startsWith("/").endsWith("/");
    }

    @Test
    void create_rejectsRootAtNonZeroRank() {
        OrgLevel building = level("BUILDING", 1);
        when(orgNodeRepository.existsByCode("B1")).thenReturn(false);
        when(orgLevelRepository.findById(building.getId())).thenReturn(Optional.of(building));

        assertThatThrownBy(() -> service.create("Building 1", "B1", null, building.getId(), null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_child_succeeds_whenRankIsParentPlusOne() {
        OrgLevel campus = level("CAMPUS", 0);
        OrgLevel building = level("BUILDING", 1);
        UUID parentId = UUID.randomUUID();
        OrgNode parent = new OrgNode();
        parent.setId(parentId);
        parent.setLevel(campus);
        parent.setPath("/" + parentId + "/");

        when(orgNodeRepository.existsByCode("B1")).thenReturn(false);
        when(orgLevelRepository.findById(building.getId())).thenReturn(Optional.of(building));
        when(orgNodeRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(orgNodeRepository.save(any(OrgNode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orgNodeRepository.findByIdWithLevel(any(UUID.class))).thenAnswer(inv -> {
            OrgNode n = new OrgNode();
            n.setId(inv.getArgument(0));
            n.setLevel(building);
            n.setParent(parent);
            n.setPath(parent.getPath() + inv.getArgument(0) + "/");
            return Optional.of(n);
        });

        OrgNode result = service.create("Building 1", "B1", parentId, building.getId(), null);

        assertThat(result.getPath()).startsWith(parent.getPath());
    }

    @Test
    void create_rejectsWrongLevelRankUnderParent() {
        OrgLevel campus = level("CAMPUS", 0);
        OrgLevel floor = level("FLOOR", 2); // skips BUILDING (rank 1) - invalid directly under a Campus
        UUID parentId = UUID.randomUUID();
        OrgNode parent = new OrgNode();
        parent.setId(parentId);
        parent.setLevel(campus);
        parent.setPath("/" + parentId + "/");

        when(orgNodeRepository.existsByCode("F1")).thenReturn(false);
        when(orgLevelRepository.findById(floor.getId())).thenReturn(Optional.of(floor));
        when(orgNodeRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.create("Floor 1", "F1", parentId, floor.getId(), null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsDuplicateCode() {
        when(orgNodeRepository.existsByCode("DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Dup", "DUP", null, UUID.randomUUID(), null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_rejectsUnconfiguredRoomVariant() {
        OrgLevel room = level("ROOM", 3);
        room.setRoomVariants(List.of("Classroom", "Laboratory"));
        UUID parentId = UUID.randomUUID();
        OrgLevel floor = level("FLOOR", 2);
        OrgNode parent = new OrgNode();
        parent.setId(parentId);
        parent.setLevel(floor);
        parent.setPath("/" + parentId + "/");

        when(orgNodeRepository.existsByCode("R1")).thenReturn(false);
        when(orgLevelRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(orgNodeRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.create("Room 1", "R1", parentId, room.getId(), "Gymnasium"))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void get_rejectsUnknownNode() {
        UUID id = UUID.randomUUID();
        when(orgNodeRepository.findByIdWithLevel(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void renameLevel_succeeds() {
        OrgLevel level = level("CAMPUS", 0);
        when(orgLevelRepository.findById(level.getId())).thenReturn(Optional.of(level));
        when(orgLevelRepository.saveAndFlush(level)).thenReturn(level);

        OrgLevel result = service.renameLevel(level.getId(), "Parish", 0L);

        assertThat(result.getName()).isEqualTo("Parish");
    }

    @Test
    void renameLevel_rejectsStaleVersion() {
        OrgLevel level = level("CAMPUS", 0);
        level.setVersion(5L);
        when(orgLevelRepository.findById(level.getId())).thenReturn(Optional.of(level));

        assertThatThrownBy(() -> service.renameLevel(level.getId(), "Parish", 2L))
                .isInstanceOf(OptimisticLockConflictException.class);
    }

    @Test
    void delete_succeeds_whenNoDependents() {
        UUID id = UUID.randomUUID();
        OrgNode node = new OrgNode();
        node.setId(id);
        node.setName("Empty Room");
        node.setLevel(level("ROOM", 3));
        when(orgNodeRepository.findByIdWithLevel(id)).thenReturn(Optional.of(node));
        when(orgNodeRepository.existsByParentId(id)).thenReturn(false);
        when(assetRepository.existsByOrgNodeId(id)).thenReturn(false);
        when(personRepository.existsByOrgNodeId(id)).thenReturn(false);
        when(appUserRepository.existsByOrgScopeNodeId(id)).thenReturn(false);

        service.delete(id);
        // no exception - success
    }

    @Test
    void delete_blocked_whenChildrenExist() {
        UUID id = UUID.randomUUID();
        OrgNode node = new OrgNode();
        node.setId(id);
        node.setName("Campus");
        node.setLevel(level("CAMPUS", 0));
        when(orgNodeRepository.findByIdWithLevel(id)).thenReturn(Optional.of(node));
        when(orgNodeRepository.existsByParentId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_blocked_whenAssetsReferenceNode() {
        UUID id = UUID.randomUUID();
        OrgNode node = new OrgNode();
        node.setId(id);
        node.setName("Room");
        node.setLevel(level("ROOM", 3));
        when(orgNodeRepository.findByIdWithLevel(id)).thenReturn(Optional.of(node));
        when(orgNodeRepository.existsByParentId(id)).thenReturn(false);
        when(assetRepository.existsByOrgNodeId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("assets");
    }
}
