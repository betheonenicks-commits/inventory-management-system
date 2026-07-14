package com.iams.org.application;

import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgLevel;
import com.iams.org.domain.OrgLevelRepository;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.PersonRepository;
import com.iams.usr.domain.AppUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-ORG-01 (build the hierarchy), US-ORG-02 (relabel level names), US-ORG-06
 * (Room-level variants). Nodes are not re-parented in this phase - only
 * create/read/delete - so `OrgNode.path` (a materialized ancestor-id chain
 * including self) is computed once at creation and never recomputed. This is
 * what makes descendant-scope matching (OrgScopeGuard) a plain prefix match
 * instead of a recursive query.
 */
@Service
public class OrgHierarchyService {

    private final OrgNodeRepository orgNodeRepository;
    private final OrgLevelRepository orgLevelRepository;
    private final AssetRepository assetRepository;
    private final PersonRepository personRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;

    public OrgHierarchyService(OrgNodeRepository orgNodeRepository, OrgLevelRepository orgLevelRepository,
                                AssetRepository assetRepository, PersonRepository personRepository,
                                AppUserRepository appUserRepository, CurrentUserProvider currentUserProvider) {
        this.orgNodeRepository = orgNodeRepository;
        this.orgLevelRepository = orgLevelRepository;
        this.assetRepository = assetRepository;
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<OrgLevel> listLevels() {
        return orgLevelRepository.findAllByOrderByRankAsc();
    }

    @Transactional
    public OrgLevel renameLevel(UUID id, String name, long expectedVersion) {
        OrgLevel level = orgLevelRepository.findById(id).orElseThrow(() -> NotFoundException.of("OrgLevel", id));
        if (level.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, level.getVersion(), level);
        }
        level.setName(name);
        try {
            return orgLevelRepository.saveAndFlush(level);
        } catch (OptimisticLockingFailureException e) {
            OrgLevel current = orgLevelRepository.findById(id).orElseThrow(() -> NotFoundException.of("OrgLevel", id));
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }
    }

    @Transactional(readOnly = true)
    public List<OrgNode> list() {
        return orgNodeRepository.findAllWithLevelOrderByPath();
    }

    @Transactional(readOnly = true)
    public OrgNode get(UUID id) {
        return orgNodeRepository.findByIdWithLevel(id).orElseThrow(() -> NotFoundException.of("OrgNode", id));
    }

    @Transactional
    public OrgNode create(String name, String code, UUID parentId, UUID levelId, String roomVariant) {
        if (orgNodeRepository.existsByCode(code)) {
            throw ValidationFailedException.singleField("code", "This code is already in use");
        }
        OrgLevel level = orgLevelRepository.findById(levelId)
                .orElseThrow(() -> NotFoundException.of("OrgLevel", levelId));

        OrgNode parent = null;
        String path;
        if (parentId != null) {
            OrgNode resolvedParent = orgNodeRepository.findById(parentId)
                    .orElseThrow(() -> NotFoundException.of("OrgNode", parentId));
            // .getLevel() is a lazy proxy, but this is still inside the transaction that
            // loaded resolvedParent - safe to touch here, unlike a mapper reading it after
            // the transaction has returned (see OrgNodeRepository's JOIN FETCH comment).
            OrgLevel parentLevel = resolvedParent.getLevel();
            if (level.getRank() != parentLevel.getRank() + 1) {
                throw ValidationFailedException.singleField("levelId",
                        "A child of '" + parentLevel.getName() + "' must be at the next level down");
            }
            path = resolvedParent.getPath();
            parent = resolvedParent;
        } else {
            if (level.getRank() != 0) {
                throw ValidationFailedException.singleField("parentId",
                        "Only a level-0 (root) node may be created without a parent");
            }
            path = "/";
        }

        if (roomVariant != null && !level.getRoomVariants().contains(roomVariant)) {
            throw ValidationFailedException.singleField("roomVariant",
                    "'" + roomVariant + "' is not a configured variant of level '" + level.getName() + "'");
        }

        OrgNode node = new OrgNode();
        node.setId(UUID.randomUUID());
        node.setName(name);
        node.setCode(code);
        node.setActive(true);
        node.setParent(parent);
        node.setLevel(level);
        node.setRoomVariant(roomVariant);
        node.setPath(path + node.getId() + "/");

        OrgNode saved = orgNodeRepository.save(node);
        return get(saved.getId());
    }

    @Transactional
    public void delete(UUID id) {
        OrgNode node = get(id);

        List<String> blockers = new ArrayList<>();
        if (orgNodeRepository.existsByParentId(id)) {
            blockers.add("one or more child org nodes");
        }
        if (assetRepository.existsByOrgNodeId(id)) {
            blockers.add("one or more assets");
        }
        if (personRepository.existsByOrgNodeId(id)) {
            blockers.add("one or more persons");
        }
        if (appUserRepository.existsByOrgScopeNodeId(id)) {
            blockers.add("one or more users scoped to it");
        }
        if (!blockers.isEmpty()) {
            throw new ConflictException("ORG_NODE_HAS_DEPENDENTS",
                    "Org node '" + node.getName() + "' cannot be deleted: still referenced by " + String.join(", ", blockers) + ".");
        }

        orgNodeRepository.delete(node);
    }
}
