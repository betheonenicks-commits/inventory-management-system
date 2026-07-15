package com.iams.dashboard.application;

import com.iams.common.security.CurrentUserProvider;
import com.iams.dashboard.domain.DashboardPreference;
import com.iams.dashboard.domain.DashboardPreferenceRepository;
import com.iams.dashboard.domain.DashboardTile;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-DSH-06: each user's own KPI tile selection. Always operates on the
 * *calling* user - there is deliberately no admin path to read or write
 * someone else's dashboard layout, so no id parameter exists to abuse.
 * <p>
 * The "sensible role-appropriate default set" for a user who has never saved
 * anything is every available tile: tile availability itself already tracks
 * the role (all six ride on dashboards:read - see DashboardTile's Javadoc),
 * so the default degrades with the role's access rather than hardcoding a
 * per-role list that would drift as roles are customized (US-USR-02 allows
 * custom roles this table can't anticipate).
 */
@Service
public class DashboardPreferenceService {

    private final DashboardPreferenceRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public DashboardPreferenceService(DashboardPreferenceRepository repository,
                                       CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Preferences current() {
        UUID userId = currentUserProvider.current().id();
        return repository.findByUserId(userId)
                .map(pref -> new Preferences(pref.getTiles().stream().map(DashboardTile::valueOf).toList(), true))
                .orElseGet(() -> new Preferences(List.of(DashboardTile.values()), false));
    }

    /**
     * Saves the calling user's tile subset. Duplicates are collapsed (first
     * occurrence wins - order is the render order); an empty list is a valid,
     * deliberate "show nothing" choice, distinguishable from never-configured.
     * Unknown tile names never reach here - the request DTO types the list as
     * DashboardTile, so JSON deserialization rejects them with a 400 first,
     * the same enum-at-the-boundary approach AssetCondition established.
     */
    @Transactional
    public Preferences save(List<DashboardTile> tiles) {
        UUID userId = currentUserProvider.current().id();
        List<DashboardTile> deduped = List.copyOf(new LinkedHashSet<>(tiles));
        DashboardPreference pref = repository.findByUserId(userId).orElseGet(() -> {
            DashboardPreference created = new DashboardPreference();
            created.setUserId(userId);
            created.setCreatedBy(userId);
            return created;
        });
        pref.setTiles(deduped.stream().map(Enum::name).toList());
        pref.setUpdatedBy(userId);
        repository.save(pref);
        return new Preferences(deduped, true);
    }

    public record Preferences(List<DashboardTile> tiles, boolean configured) {
    }
}
