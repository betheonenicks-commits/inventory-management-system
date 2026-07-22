package com.iams.platform.api;

import com.iams.platform.api.dto.SystemHealthResponse;
import com.iams.platform.application.SystemHealthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-USR-05 (AC-USR-05-H): the System Operator's technical-configuration
 * surface. Backup and LDAP settings have no underlying subsystem built yet
 * (no backup-execution or LDAP-integration code exists anywhere in this
 * codebase) - only system health is real today; adding stub endpoints for
 * the other two would be fabricating functionality this session hasn't
 * actually built.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final SystemHealthService systemHealthService;

    public SystemController(SystemHealthService systemHealthService) {
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/health")
    @PreAuthorize("@perm.has('system:read')")
    public SystemHealthResponse health() {
        return systemHealthService.check();
    }
}
