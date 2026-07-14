package com.iams.compliance.api;

import com.iams.compliance.api.dto.PersonAnonymizationResponse;
import com.iams.compliance.application.PersonAnonymizationService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-CMP-02 / US-LIF-14: flag-eligible departed persons, anonymize on explicit approval. */
@RestController
@RequestMapping("/api/v1/compliance/person-anonymization")
public class PersonAnonymizationController {

    private final PersonAnonymizationService anonymizationService;
    private final ComplianceMapper mapper;

    public PersonAnonymizationController(PersonAnonymizationService anonymizationService, ComplianceMapper mapper) {
        this.anonymizationService = anonymizationService;
        this.mapper = mapper;
    }

    @GetMapping("/eligible")
    @PreAuthorize("@perm.has('compliance:read')")
    public List<PersonAnonymizationResponse> eligible() {
        return anonymizationService.eligible().stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{personId}/anonymize")
    @PreAuthorize("@perm.has('compliance:write')")
    public PersonAnonymizationResponse anonymize(@PathVariable UUID personId) {
        return mapper.toResponse(anonymizationService.anonymize(personId));
    }
}
