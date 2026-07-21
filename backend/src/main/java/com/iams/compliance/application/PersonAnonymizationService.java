package com.iams.compliance.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.audit.domain.AuditRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.api.dto.PersonDataExportResponse;
import com.iams.compliance.domain.LegalHoldScopeType;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.AppUserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-CMP-02: a departed person becomes eligible once marked inactive
 * ({@link Person#isActive()} false); anonymization itself only happens on
 * explicit Compliance Officer approval (deliberate, not automatic - AC's own
 * wording), gated the same way everything else compliance:write-scoped is.
 * <p>
 * Blocked-while-assigned (AC-CMP-02-X) reuses {@code AssetRepository.findByAssignedToPersonId} -
 * the exact same check and response shape {@code UserDeactivationService}
 * already established for US-USR-08 - and is simultaneously what closes
 * US-LIF-14 ("block a person's erasure while assets remain assigned"),
 * since anonymization is this codebase's actual erasure mechanism for a
 * Person (there is no separate hard-delete endpoint, by design - see
 * PersonService's own comment on why delete was never built).
 */
@Service
public class PersonAnonymizationService {

    private static final String ANONYMIZED_NAME = "Anonymized Person";

    private final PersonRepository personRepository;
    private final AssetRepository assetRepository;
    private final AppUserRepository appUserRepository;
    private final AuditRepository auditRepository;
    private final LegalHoldService legalHoldService;
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;

    public PersonAnonymizationService(PersonRepository personRepository, AssetRepository assetRepository,
                                       AppUserRepository appUserRepository, AuditRepository auditRepository,
                                       LegalHoldService legalHoldService, CurrentUserProvider currentUserProvider,
                                       SecurityEventLogger securityEventLogger) {
        this.personRepository = personRepository;
        this.assetRepository = assetRepository;
        this.appUserRepository = appUserRepository;
        this.auditRepository = auditRepository;
        this.legalHoldService = legalHoldService;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
    }

    @Transactional(readOnly = true)
    public List<Person> eligible() {
        return personRepository.findAllByOrderByFullNameAsc().stream()
                .filter(p -> !p.isActive() && p.getAnonymizedAt() == null)
                .toList();
    }

    /**
     * US-SEC-10 (AC-SEC-10-H): "an export was available beforehand" - a Compliance Officer
     * pulls this before running erasure. Deliberately available regardless of eligibility
     * (an export is harmless read access; only anonymize() itself needs the departed/no-holds
     * gating) so a request can be honored even before someone is marked inactive.
     */
    @Transactional(readOnly = true)
    public PersonDataExportResponse exportData(UUID personId) {
        Person person = personRepository.findById(personId).orElseThrow(() -> NotFoundException.of("Person", personId));
        List<PersonDataExportResponse.AssignedAsset> assignedAssets =
                assetRepository.findByAssignedToPersonId(personId).stream()
                        .map(a -> new PersonDataExportResponse.AssignedAsset(a.getId(), a.getAssetNumber(), a.getName()))
                        .toList();
        return new PersonDataExportResponse(
                person.getId(), person.getFullName(), person.getEmail(),
                person.getPersonType() != null ? person.getPersonType().name() : null,
                person.getOrgNode() != null ? person.getOrgNode().getId() : null,
                person.getOrgNode() != null ? person.getOrgNode().getName() : null,
                person.getDepartmentId(), person.isActive(), person.getCreatedAt(), person.getUpdatedAt(),
                assignedAssets, Instant.now());
    }

    @Transactional
    public Person anonymize(UUID personId) {
        Person person = personRepository.findById(personId).orElseThrow(() -> NotFoundException.of("Person", personId));
        if (person.isActive()) {
            // AC-CMP-02: only a departed (inactive) person is eligible in the first place.
            throw new ConflictException("PERSON_STILL_ACTIVE", "Only an inactive (departed) person can be anonymized");
        }
        if (person.getAnonymizedAt() != null) {
            throw new ConflictException("ALREADY_ANONYMIZED", "This person has already been anonymized");
        }

        List<Asset> blockingAssets = assetRepository.findByAssignedToPersonId(personId);
        if (!blockingAssets.isEmpty()) {
            // AC-CMP-02-X / US-LIF-14: blocked with the asset list, same shape UserDeactivationService uses.
            throw blockedByAssignedAssets(blockingAssets);
        }
        requireNoHoldOnALinkedAudit(personId);

        UUID actor = currentUserProvider.current().id();
        person.setFullName(ANONYMIZED_NAME);
        person.setEmail(null);
        person.setAnonymizedAt(Instant.now());
        person.setUpdatedBy(actor);
        Person saved = personRepository.saveAndFlush(person);
        // The person's id is preserved (the "stable pseudonym" AC-CMP-02-H requires) -
        // historical audit findings/history rows that reference this id still resolve.
        securityEventLogger.record(SecurityEventType.PERSON_ANONYMIZED, actor, null, null, "Anonymized person " + personId);
        return saved;
    }

    /**
     * AC-SEC-10-X: "an active legal hold on a linked audit" blocks anonymization (423) - a
     * departed person may also have held a login account (AppUser.personId) that acted as an
     * audit's submitter, approver, or an assigned auditor; if any such audit is currently
     * under legal hold, anonymizing this person is refused until the hold is lifted. Most
     * departed persons never had a login at all, so this is a no-op for the common case.
     */
    private void requireNoHoldOnALinkedAudit(UUID personId) {
        Optional<AppUser> linkedUser = appUserRepository.findByPersonId(personId);
        if (linkedUser.isEmpty()) {
            return;
        }
        for (UUID auditId : auditRepository.findAuditIdsLinkedToUser(linkedUser.get().getId())) {
            legalHoldService.requireNoActiveHold(LegalHoldScopeType.AUDIT, auditId);
        }
    }

    private ConflictException blockedByAssignedAssets(List<Asset> blockingAssets) {
        List<Map<String, Object>> blockingAssetPayload = blockingAssets.stream().map(asset -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("assetId", asset.getId());
            entry.put("assetNumber", asset.getAssetNumber());
            entry.put("name", asset.getName());
            return entry;
        }).toList();

        Map<String, Object> extraProperties = new LinkedHashMap<>();
        extraProperties.put("blockingAssets", blockingAssetPayload);
        extraProperties.put("resolutionActions", List.of(
                "POST /api/v1/assets/{assetId}/assign — reassign to another holder",
                "POST /api/v1/assets/{assetId}/return-to-inventory — return, awaiting reissue"));

        return new ConflictException(
                "PERSON_HAS_OUTSTANDING_ASSIGNMENTS",
                "Cannot anonymize this person",
                blockingAssets.size() + " assets are currently assigned to this person and must be reassigned "
                        + "or returned to inventory before anonymization.",
                extraProperties);
    }
}
