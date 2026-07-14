package com.iams.compliance.api;

import com.iams.compliance.api.dto.PrivacyNoticeConfigRequest;
import com.iams.compliance.api.dto.PrivacyNoticeConfigResponse;
import com.iams.compliance.application.PrivacyNoticeConfigService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-CMP-03: privacy-notice text per personal-data field. */
@RestController
@RequestMapping("/api/v1/compliance/privacy-notices")
public class PrivacyNoticeConfigController {

    private final PrivacyNoticeConfigService configService;
    private final ComplianceMapper mapper;

    public PrivacyNoticeConfigController(PrivacyNoticeConfigService configService, ComplianceMapper mapper) {
        this.configService = configService;
        this.mapper = mapper;
    }

    @PutMapping
    @PreAuthorize("@perm.has('compliance:write')")
    public PrivacyNoticeConfigResponse save(@Valid @RequestBody PrivacyNoticeConfigRequest request) {
        return mapper.toResponse(configService.save(request.fieldName(), request.noticeText()));
    }

    @GetMapping
    public List<PrivacyNoticeConfigResponse> list() {
        return configService.list().stream().map(mapper::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('compliance:write')")
    public void delete(@PathVariable UUID id) {
        configService.delete(id);
    }
}
