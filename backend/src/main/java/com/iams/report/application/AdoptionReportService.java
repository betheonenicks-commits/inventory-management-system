package com.iams.report.application;

import com.iams.analytics.domain.AdoptionAggregate;
import com.iams.analytics.domain.UsageEventRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.RoleRepository;
import com.iams.usr.domain.UserRoleAssignmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-ANL-03: feature adoption by role x module. The matrix is EXPECTATION-
 * driven, not observation-driven: a role's row for a module it is expected
 * to use (derived from the role's own permission grants, so a custom role
 * lands in the right cells automatically) appears even when - especially
 * when - there is zero usage. That is the AC's "gap is visible, not
 * averaged away": absence produces a flagged GAP row, never a missing row.
 * <p>
 * Only roles with at least one current assignment appear: a role nobody
 * holds cannot have adoption, and its all-GAP rows would be noise about
 * staffing, not training or UX.
 */
@Service
public class AdoptionReportService {

    /** Expected usage in the period but fewer events than this - flagged NEAR-ZERO rather than silently "some". */
    static final int NEAR_ZERO_THRESHOLD = 5;
    private static final int MAX_PERIOD_DAYS = 730;

    /**
     * Tracked module -> permissions any one of which marks a role as expected
     * to use it. An empty list means every role is expected (search has no
     * permission gate by design). Kept in step with the @TrackUsage catalog -
     * a module tracked but absent here still shows up in observed rows, just
     * without expectation flagging.
     */
    private static final Map<String, List<String>> MODULE_EXPECTATIONS = new LinkedHashMap<>();
    static {
        MODULE_EXPECTATIONS.put("search", List.of());
        MODULE_EXPECTATIONS.put("dashboard", List.of("dashboards:read"));
        MODULE_EXPECTATIONS.put("assets", List.of("assets:read", "assets:read:own"));
        MODULE_EXPECTATIONS.put("audits", List.of("audits:read"));
        MODULE_EXPECTATIONS.put("inventory", List.of("inventory:read"));
        MODULE_EXPECTATIONS.put("reports", List.of("reports:read"));
    }

    private final UsageEventRepository usageEventRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;

    public AdoptionReportService(UsageEventRepository usageEventRepository, RoleRepository roleRepository,
                                 UserRoleAssignmentRepository assignmentRepository) {
        this.usageEventRepository = usageEventRepository;
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public TabularReport usageAdoption(int withinDays) {
        if (withinDays < 1 || withinDays > MAX_PERIOD_DAYS) {
            throw ValidationFailedException.singleField("withinDays", "Must be between 1 and " + MAX_PERIOD_DAYS);
        }
        Instant since = Instant.now().minus(Duration.ofDays(withinDays));

        // (role, module) -> aggregate, from one grouped query.
        Map<String, Map<String, AdoptionAggregate>> observed = new HashMap<>();
        for (AdoptionAggregate aggregate : usageEventRepository.aggregateSince(since)) {
            observed.computeIfAbsent(aggregate.role(), r -> new HashMap<>()).put(aggregate.module(), aggregate);
        }

        List<Role> heldRoles = roleRepository.findAll().stream()
                .filter(role -> !assignmentRepository.findByRoleId(role.getId()).isEmpty())
                .sorted(Comparator.comparing(Role::getCode))
                .toList();

        List<List<String>> rows = new ArrayList<>();
        for (Role role : heldRoles) {
            Map<String, AdoptionAggregate> byModule = observed.getOrDefault(role.getCode(), Map.of());
            for (Map.Entry<String, List<String>> entry : MODULE_EXPECTATIONS.entrySet()) {
                boolean expected = isExpected(role, entry.getValue());
                AdoptionAggregate aggregate = byModule.get(entry.getKey());
                if (!expected && aggregate == null) {
                    // Neither expected nor used: a row here would state nothing.
                    continue;
                }
                rows.add(row(role.getCode(), entry.getKey(), aggregate, expected));
            }
            // Tracked modules outside the expectation catalog (e.g. "feedback"):
            // observed-only, no expectation to judge against.
            byModule.keySet().stream()
                    .filter(module -> !MODULE_EXPECTATIONS.containsKey(module))
                    .sorted()
                    .forEach(module -> rows.add(row(role.getCode(), module, byModule.get(module), false)));
        }

        return new TabularReport("usage-adoption",
                "Feature Adoption by Role & Module (last " + withinDays + " days, roles currently held)",
                Instant.now(),
                List.of("Role", "Module", "Events", "Distinct Users", "Last Used", "Adoption"), rows);
    }

    /** A wildcard-permission role (SUPER_ADMIN) is expected everywhere; an empty requirement list means everyone is. */
    private static boolean isExpected(Role role, List<String> anyOfPermissions) {
        if (anyOfPermissions.isEmpty() || role.getPermissions().contains("*")) {
            return true;
        }
        return anyOfPermissions.stream().anyMatch(role.getPermissions()::contains);
    }

    private static List<String> row(String roleCode, String module, AdoptionAggregate aggregate, boolean expected) {
        long events = aggregate == null ? 0 : aggregate.events();
        String status;
        if (!expected) {
            status = "-";
        } else if (events == 0) {
            status = "GAP (expected, no usage)";
        } else if (events < NEAR_ZERO_THRESHOLD) {
            status = "NEAR-ZERO (expected)";
        } else {
            status = "OK";
        }
        return List.of(roleCode, module,
                String.valueOf(events),
                aggregate == null ? "0" : String.valueOf(aggregate.distinctUsers()),
                aggregate == null ? "" : DateTimeFormatter.ISO_INSTANT.format(aggregate.lastUsed().truncatedTo(ChronoUnit.SECONDS)),
                status);
    }
}
