package com.iams.compliance.application;

import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.PrivacyNoticeConfig;
import com.iams.compliance.domain.PrivacyNoticeConfigRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-CMP-03: privacy-notice text per personal-data field. */
@Service
public class PrivacyNoticeConfigService {

    private final PrivacyNoticeConfigRepository configRepository;
    private final CurrentUserProvider currentUserProvider;

    public PrivacyNoticeConfigService(PrivacyNoticeConfigRepository configRepository, CurrentUserProvider currentUserProvider) {
        this.configRepository = configRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public PrivacyNoticeConfig save(String fieldName, String noticeText) {
        if (fieldName == null || fieldName.isBlank()) {
            throw ValidationFailedException.singleField("fieldName", "This field is required");
        }
        if (noticeText == null || noticeText.isBlank()) {
            throw ValidationFailedException.singleField("noticeText", "This field is required");
        }

        PrivacyNoticeConfig config = configRepository.findByFieldName(fieldName).orElseGet(PrivacyNoticeConfig::new);
        UUID actor = currentUserProvider.current().id();
        boolean isNew = config.getId() == null;
        config.setFieldName(fieldName);
        config.setNoticeText(noticeText);
        if (isNew) {
            config.setCreatedBy(actor);
        } else {
            config.setUpdatedBy(actor);
        }
        return configRepository.save(config);
    }

    @Transactional(readOnly = true)
    public Optional<PrivacyNoticeConfig> get(String fieldName) {
        return configRepository.findByFieldName(fieldName);
    }

    @Transactional(readOnly = true)
    public List<PrivacyNoticeConfig> list() {
        return configRepository.findAllByOrderByFieldNameAsc();
    }

    @Transactional
    public void delete(UUID id) {
        if (!configRepository.existsById(id)) {
            throw NotFoundException.of("PrivacyNoticeConfig", id);
        }
        configRepository.deleteById(id);
    }
}
