package com.iams.compliance.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.OutboundIntegrationFlag;
import com.iams.compliance.domain.OutboundIntegrationFlagRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-CMP-05: "a single view confirming all data stores are on-premises and
 * flagging any enabled outbound integration." On-premises confirmation is
 * hardcoded true - BR-15 ("operate fully on-premises with no mandatory
 * external dependencies") is this product's own fixed architecture, not a
 * per-deployment setting stored in a database row. The outbound-flow
 * registry is real and mutable (a Compliance Officer can register/enable
 * one, e.g. "ACCOUNTING_EXPORT" per the story's own example) even though no
 * EPIC-INT integration exists yet to actually drive traffic through it.
 */
@Service
public class DataResidencyService {

    private final OutboundIntegrationFlagRepository flagRepository;
    private final CurrentUserProvider currentUserProvider;

    public DataResidencyService(OutboundIntegrationFlagRepository flagRepository, CurrentUserProvider currentUserProvider) {
        this.flagRepository = flagRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public DataResidencyView view() {
        return new DataResidencyView(true, flagRepository.findByEnabledTrue());
    }

    @Transactional(readOnly = true)
    public List<OutboundIntegrationFlag> list() {
        return flagRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public OutboundIntegrationFlag save(String name, boolean enabled, String complianceReviewNote) {
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "This field is required");
        }

        UUID actor = currentUserProvider.current().id();
        OutboundIntegrationFlag flag = flagRepository.findAllByOrderByNameAsc().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseGet(OutboundIntegrationFlag::new);
        boolean isNew = flag.getId() == null;
        flag.setName(name);
        flag.setEnabled(enabled);
        flag.setComplianceReviewNote(complianceReviewNote);
        if (isNew) {
            flag.setCreatedBy(actor);
        } else {
            flag.setUpdatedBy(actor);
        }
        return flagRepository.save(flag);
    }

    @Transactional
    public void delete(UUID id) {
        if (!flagRepository.existsById(id)) {
            throw NotFoundException.of("OutboundIntegrationFlag", id);
        }
        flagRepository.deleteById(id);
    }
}
