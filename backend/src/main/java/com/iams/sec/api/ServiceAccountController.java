package com.iams.sec.api;

import com.iams.sec.api.dto.ServiceAccountCreateRequest;
import com.iams.sec.api.dto.ServiceAccountIssuedResponse;
import com.iams.sec.api.dto.ServiceAccountResponse;
import com.iams.sec.application.ServiceAccountService;
import com.iams.sec.domain.ServiceAccount;
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
 * US-SEC-14: Super-Administrator management of integration service accounts. Gated on
 * security:write (which SUPER_ADMIN's '*' and IT_SECURITY_OFFICER hold). The raw API
 * key is returned only on creation; listings never expose it (US-SEC-15).
 */
@RestController
@RequestMapping("/api/v1/service-accounts")
public class ServiceAccountController {

    private final ServiceAccountService service;

    public ServiceAccountController(ServiceAccountService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('security:write')")
    public ResponseEntity<ServiceAccountIssuedResponse> create(@Valid @RequestBody ServiceAccountCreateRequest request) {
        ServiceAccountService.Issued issued = service.create(request.name(), request.description(), request.scopes());
        ServiceAccountIssuedResponse body = new ServiceAccountIssuedResponse(toResponse(issued.account()), issued.rawApiKey());
        return ResponseEntity.created(URI.create("/api/v1/service-accounts/" + issued.account().getId())).body(body);
    }

    @GetMapping
    @PreAuthorize("@perm.has('security:read')")
    public List<ServiceAccountResponse> list() {
        return service.list().stream().map(ServiceAccountController::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('security:write')")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        service.revoke(id);
        return ResponseEntity.noContent().build();
    }

    private static ServiceAccountResponse toResponse(ServiceAccount a) {
        return new ServiceAccountResponse(a.getId(), a.getName(), a.getDescription(), a.getApiKeyPrefix(),
                a.getScopes(), a.isActive(), a.getLastUsedAt(), a.getCreatedAt());
    }
}
