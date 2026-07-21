package com.iams.integration.api;

import com.iams.integration.api.dto.IntegrationCreateRequest;
import com.iams.integration.api.dto.IntegrationResponse;
import com.iams.integration.application.IntegrationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-SEC-15 / FR-INT-05: IT Security Officer / Super-Administrator management of the
 * integration registry. Gated on security:write (create/enable/disable/delete) and
 * security:read (list/get) - the same gate as service accounts, since an integration's
 * credential reference is a security-sensitive setting. The credential is only ever a
 * secrets-manager reference; the service refuses an inline secret at 400 (AC-SEC-15-X).
 */
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationService service;

    public IntegrationController(IntegrationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('security:write')")
    public ResponseEntity<IntegrationResponse> create(@Valid @RequestBody IntegrationCreateRequest request) {
        IntegrationResponse body = IntegrationResponse.from(service.create(request.name(), request.type(),
                request.description(), request.credentialRef(), request.config()));
        return ResponseEntity.created(URI.create("/api/v1/integrations/" + body.id())).body(body);
    }

    @GetMapping
    @PreAuthorize("@perm.has('security:read')")
    public List<IntegrationResponse> list() {
        return service.list().stream().map(IntegrationResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('security:read')")
    public IntegrationResponse get(@PathVariable UUID id) {
        return IntegrationResponse.from(service.get(id));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("@perm.has('security:write')")
    public IntegrationResponse enable(@PathVariable UUID id) {
        return IntegrationResponse.from(service.setEnabled(id, true));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("@perm.has('security:write')")
    public IntegrationResponse disable(@PathVariable UUID id) {
        return IntegrationResponse.from(service.setEnabled(id, false));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('security:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
