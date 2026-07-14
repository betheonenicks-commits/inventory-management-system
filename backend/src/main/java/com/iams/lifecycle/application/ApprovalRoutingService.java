package com.iams.lifecycle.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.lifecycle.domain.ApprovalDelegation;
import com.iams.lifecycle.domain.ApprovalDelegationRepository;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignment;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-LIF-15 (delegation) and the delegate-first half of US-LIF-13
 * (escalation). Shared by TransferService and DisposalService rather than
 * duplicated, since both route approvals through the same logic.
 * <p>
 * "Route to the configured approver" (US-LIF-01/05) has no automatic
 * node-to-approver resolution anywhere in this codebase - Department has no
 * head/manager field, OrgNode has no approver field. Every approver here is
 * caller-supplied at request-creation time, the same pattern
 * Audit.nominalApproverId already established.
 */
@Service
public class ApprovalRoutingService {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final ApprovalDelegationRepository delegationRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public ApprovalRoutingService(ApprovalDelegationRepository delegationRepository, AppUserRepository appUserRepository,
                                   RoleRepository roleRepository, UserRoleAssignmentRepository roleAssignmentRepository,
                                   CurrentUserProvider currentUserProvider) {
        this.delegationRepository = delegationRepository;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ApprovalDelegation createDelegation(UUID delegateUserId, Instant validFrom, Instant validTo, String reason) {
        UUID delegatorUserId = currentUserProvider.current().id();
        if (delegateUserId.equals(delegatorUserId)) {
            throw ValidationFailedException.singleField("delegateUserId", "Cannot delegate approval authority to yourself");
        }
        if (!appUserRepository.existsById(delegateUserId)) {
            throw NotFoundException.of("AppUser", delegateUserId);
        }
        if (!validTo.isAfter(validFrom)) {
            throw ValidationFailedException.singleField("validTo", "must be after validFrom");
        }

        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setDelegatorUserId(delegatorUserId);
        delegation.setDelegateUserId(delegateUserId);
        delegation.setValidFrom(validFrom);
        delegation.setValidTo(validTo);
        delegation.setReason(reason);
        delegation.setActive(true);
        delegation.setCreatedBy(delegatorUserId);
        return delegationRepository.save(delegation);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDelegation> list(UUID delegatorUserId) {
        return delegationRepository.findByDelegatorUserIdOrderByValidFromDesc(delegatorUserId);
    }

    @Transactional
    public ApprovalDelegation revoke(UUID id) {
        ApprovalDelegation delegation = delegationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("ApprovalDelegation", id));
        delegation.setActive(false);
        delegation.setUpdatedBy(currentUserProvider.current().id());
        return delegationRepository.saveAndFlush(delegation);
    }

    /** US-LIF-15 AC: "routes to my delegate instead" while a delegation window is active, else unchanged. */
    @Transactional(readOnly = true)
    public UUID resolveEffectiveApprover(UUID nominalApproverId) {
        return delegationRepository.findActiveDelegation(nominalApproverId, Instant.now())
                .map(ApprovalDelegation::getDelegateUserId)
                .orElse(nominalApproverId);
    }

    /**
     * US-LIF-13 escalation target: the current approver's active delegate if
     * one exists, else any Administrator - "Department Head's line manager"
     * is skipped, since no management-hierarchy concept exists anywhere in
     * this codebase to resolve it from (see class Javadoc). Documented gap,
     * not a silent shortcut.
     */
    @Transactional(readOnly = true)
    public UUID resolveEscalationTarget(UUID currentApproverId) {
        Optional<ApprovalDelegation> delegate = delegationRepository.findActiveDelegation(currentApproverId, Instant.now());
        if (delegate.isPresent()) {
            return delegate.get().getDelegateUserId();
        }
        Role adminRole = roleRepository.findByCode(ADMIN_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing from seed data"));
        List<UserRoleAssignment> admins = roleAssignmentRepository.findByRoleId(adminRole.getId());
        if (admins.isEmpty()) {
            throw new IllegalStateException("No Administrator account exists to escalate to");
        }
        return admins.get(0).getUser().getId();
    }
}
