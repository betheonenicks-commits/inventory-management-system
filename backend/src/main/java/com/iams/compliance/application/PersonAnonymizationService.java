package com.iams.compliance.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final CurrentUserProvider currentUserProvider;
    private final SecurityEventLogger securityEventLogger;

    public PersonAnonymizationService(PersonRepository personRepository, AssetRepository assetRepository,
                                       CurrentUserProvider currentUserProvider, SecurityEventLogger securityEventLogger) {
        this.personRepository = personRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
        this.securityEventLogger = securityEventLogger;
    }

    @Transactional(readOnly = true)
    public List<Person> eligible() {
        return personRepository.findAllByOrderByFullNameAsc().stream()
                .filter(p -> !p.isActive() && p.getAnonymizedAt() == null)
                .toList();
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
