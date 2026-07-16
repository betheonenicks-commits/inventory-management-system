package com.iams.notification.application;

import com.iams.lifecycle.application.ApprovalRoutingService;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-NTF-07: recipients resolved by role x scope AT SEND TIME - no stored
 * recipient list anywhere, so an org change or role reassignment is
 * reflected on the very next event. Delegation is honored by mapping each
 * resolved user through the same ApprovalRoutingService the approval flows
 * use, so "Dept Head of scope" reaches an active delegate exactly when an
 * approval would.
 */
@Service
public class RecipientResolverService {

    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final AppUserRepository userRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final ApprovalRoutingService approvalRoutingService;

    public RecipientResolverService(RoleRepository roleRepository, UserRoleAssignmentRepository assignmentRepository,
                                    AppUserRepository userRepository, OrgNodeRepository orgNodeRepository,
                                    ApprovalRoutingService approvalRoutingService) {
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.approvalRoutingService = approvalRoutingService;
    }

    @Transactional(readOnly = true)
    public Set<UUID> roleHoldersCoveringScope(String roleCode, UUID orgNodeId) {
        String targetPath = orgNodeId == null ? null
                : orgNodeRepository.findById(orgNodeId).map(OrgNode::getPath).orElse(null);
        Role role = roleRepository.findByCode(roleCode).orElse(null);
        if (role == null) {
            return Set.of();
        }
        return assignmentRepository.findByRoleId(role.getId()).stream()
                .map(a -> a.getUser().getId())
                .filter(userId -> covers(userId, targetPath))
                .map(approvalRoutingService::resolveEffectiveApprover)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<UUID> admins() {
        Set<UUID> result = roleHoldersCoveringScope("ADMIN", null);
        result.addAll(roleHoldersCoveringScope("SUPER_ADMIN", null));
        return result;
    }

    /** Delegation-aware identity mapping for a single, already-known recipient (US-NTF-07's "including an active delegate"). */
    @Transactional(readOnly = true)
    public UUID effective(UUID userId) {
        return userId == null ? null : approvalRoutingService.resolveEffectiveApprover(userId);
    }

    /** A user with no org scope sees everything; otherwise their scope path must be a prefix of the target's. */
    private boolean covers(UUID userId, String targetPath) {
        if (targetPath == null) {
            return true;
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        OrgNode scope = user.getOrgScopeNode();
        return scope == null || targetPath.startsWith(scope.getPath());
    }

    @Transactional(readOnly = true)
    public List<UUID> resolveMany(List<UUID> userIds) {
        return userIds.stream().filter(java.util.Objects::nonNull)
                .map(approvalRoutingService::resolveEffectiveApprover).toList();
    }
}
