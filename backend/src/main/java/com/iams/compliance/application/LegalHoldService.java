package com.iams.compliance.application;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.LegalHold;
import com.iams.compliance.domain.LegalHoldRepository;
import com.iams.compliance.domain.LegalHoldScopeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-CMP-06: places/lifts a legal hold on an asset or audit record. Lift's
 * "only a Compliance Officer or Super Admin" restriction (AC-CMP-06-X) is
 * enforced at the controller (compliance:write - only ever granted to those
 * two per V15), not re-checked here.
 */
@Service
public class LegalHoldService {

    private final LegalHoldRepository holdRepository;
    private final CurrentUserProvider currentUserProvider;

    public LegalHoldService(LegalHoldRepository holdRepository, CurrentUserProvider currentUserProvider) {
        this.holdRepository = holdRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public LegalHold place(LegalHoldScopeType scopeType, UUID scopeId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to place a legal hold");
        }
        if (holdRepository.findByScopeTypeAndScopeIdAndActiveTrue(scopeType, scopeId).isPresent()) {
            throw new ConflictException("HOLD_ALREADY_ACTIVE", "An active legal hold already exists for this " + scopeType);
        }

        UUID actor = currentUserProvider.current().id();
        LegalHold hold = new LegalHold();
        hold.setScopeType(scopeType);
        hold.setScopeId(scopeId);
        hold.setReason(reason);
        hold.setActive(true);
        hold.setCreatedBy(actor);
        return holdRepository.save(hold);
    }

    @Transactional
    public LegalHold lift(UUID id, String liftReason) {
        if (liftReason == null || liftReason.isBlank()) {
            throw ValidationFailedException.singleField("liftReason", "A reason is required to lift a legal hold");
        }
        LegalHold hold = get(id);
        if (!hold.isActive()) {
            throw new ConflictException("HOLD_ALREADY_LIFTED", "This legal hold has already been lifted");
        }

        UUID actor = currentUserProvider.current().id();
        hold.setActive(false);
        hold.setLiftedBy(actor);
        hold.setLiftedAt(Instant.now());
        hold.setLiftReason(liftReason);
        hold.setUpdatedBy(actor);
        return holdRepository.saveAndFlush(hold);
    }

    @Transactional(readOnly = true)
    public LegalHold get(UUID id) {
        return holdRepository.findById(id).orElseThrow(() -> NotFoundException.of("LegalHold", id));
    }

    @Transactional(readOnly = true)
    public List<LegalHold> list(LegalHoldScopeType scopeType) {
        if (scopeType != null) {
            return holdRepository.findByScopeTypeOrderByCreatedAtDesc(scopeType);
        }
        return holdRepository.findAllByOrderByCreatedAtDesc();
    }

    /** AC-CMP-06-H: "retention purge or anonymization... blocked (423) until the hold is lifted." */
    @Transactional(readOnly = true)
    public void requireNoActiveHold(LegalHoldScopeType scopeType, UUID scopeId) {
        if (holdRepository.findByScopeTypeAndScopeIdAndActiveTrue(scopeType, scopeId).isPresent()) {
            throw new LegalHoldActiveException(scopeType);
        }
    }
}
