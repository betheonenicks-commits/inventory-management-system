package com.iams.usr.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import com.iams.usr.domain.SodWaiver;
import com.iams.usr.domain.SodWaiverRepository;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-USR-09: recording a Separation-of-Duties waiver. Sign-off can never be
 * self-asserted (AC-USR-09-X): the signing user must actually hold the
 * IT_SECURITY_OFFICER role, and can never be the same user who is recording
 * the waiver.
 * <p>
 * Deliberately NOT built here (no home for it yet, noted rather than
 * silently skipped): actually engaging the SoD-conflict reroute path
 * (US-AUD-22) when a waiver is active - that requires the approval workflow
 * EPIC-AUD/EPIC-LIF haven't built. {@link com.iams.usr.domain.SodWaiverRepository#existsByScopeAndActiveTrue}
 * is the lookup that reroute path will need, added now so it doesn't need a
 * schema change later.
 */
@Service
public class SodWaiverService {

    private static final String IT_SECURITY_OFFICER_ROLE_CODE = "IT_SECURITY_OFFICER";

    private final SodWaiverRepository sodWaiverRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleAssignmentRepository roleAssignmentRepository;
    private final CurrentUserProvider currentUserProvider;

    public SodWaiverService(SodWaiverRepository sodWaiverRepository, AppUserRepository appUserRepository,
                             UserRoleAssignmentRepository roleAssignmentRepository, CurrentUserProvider currentUserProvider) {
        this.sodWaiverRepository = sodWaiverRepository;
        this.appUserRepository = appUserRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<SodWaiver> list() {
        return sodWaiverRepository.findAllWithSignedOffByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public SodWaiver get(UUID id) {
        return sodWaiverRepository.findByIdWithSignedOffBy(id).orElseThrow(() -> NotFoundException.of("SodWaiver", id));
    }

    @Transactional
    public SodWaiver create(String scope, UUID signedOffByUserId, String reason) {
        UUID requesterId = currentUserProvider.current().id();
        if (signedOffByUserId.equals(requesterId)) {
            throw ValidationFailedException.singleField("signedOffByUserId",
                    "Sign-off cannot be self-asserted - it must be a different user with the IT Security Officer role");
        }

        AppUser signer = appUserRepository.findById(signedOffByUserId)
                .orElseThrow(() -> NotFoundException.of("AppUser", signedOffByUserId));
        boolean isItSecurityOfficer = roleAssignmentRepository.findByUserId(signedOffByUserId).stream()
                .anyMatch(assignment -> IT_SECURITY_OFFICER_ROLE_CODE.equals(assignment.getRole().getCode()));
        if (!isItSecurityOfficer) {
            throw ValidationFailedException.singleField("signedOffByUserId",
                    "'" + signer.getUsername() + "' does not hold the IT Security Officer role");
        }

        SodWaiver waiver = new SodWaiver();
        waiver.setScope(scope);
        waiver.setSignedOffBy(signer);
        waiver.setReason(reason);
        waiver.setActive(true);
        waiver.setCreatedBy(requesterId);

        SodWaiver saved = sodWaiverRepository.save(waiver);
        return get(saved.getId());
    }

    @Transactional
    public SodWaiver revoke(UUID id) {
        SodWaiver waiver = get(id);
        waiver.setActive(false);
        waiver.setUpdatedBy(currentUserProvider.current().id());
        return sodWaiverRepository.saveAndFlush(waiver);
    }
}
