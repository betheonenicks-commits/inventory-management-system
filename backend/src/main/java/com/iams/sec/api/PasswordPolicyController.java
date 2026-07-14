package com.iams.sec.api;

import com.iams.sec.api.dto.PasswordPolicyResponse;
import com.iams.sec.api.dto.PasswordPolicyUpdateRequest;
import com.iams.sec.application.PasswordPolicyService;
import com.iams.sec.domain.PasswordPolicy;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-SEC-05: read and configure the password policy. GET is open to any
 * authenticated user (not sensitive - a client rendering a password field
 * needs the current rules to show live validation) - only PATCH is gated.
 */
@RestController
@RequestMapping("/api/v1/security/password-policy")
public class PasswordPolicyController {

    private final PasswordPolicyService service;

    public PasswordPolicyController(PasswordPolicyService service) {
        this.service = service;
    }

    @GetMapping
    public PasswordPolicyResponse get() {
        return toResponse(service.get());
    }

    @PatchMapping
    @PreAuthorize("@perm.has('security:write')")
    public PasswordPolicyResponse update(@Valid @RequestBody PasswordPolicyUpdateRequest request) {
        return toResponse(service.update(request.minLength(), request.requireUppercase(), request.requireLowercase(),
                request.requireDigit(), request.requireSpecial(), request.version()));
    }

    private PasswordPolicyResponse toResponse(PasswordPolicy policy) {
        return new PasswordPolicyResponse(policy.getId(), policy.getVersion(), policy.getMinLength(),
                policy.isRequireUppercase(), policy.isRequireLowercase(), policy.isRequireDigit(), policy.isRequireSpecial());
    }
}
