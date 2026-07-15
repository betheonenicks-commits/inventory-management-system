package com.iams.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.dashboard.domain.DashboardPreference;
import com.iams.dashboard.domain.DashboardPreferenceRepository;
import com.iams.dashboard.domain.DashboardTile;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardPreferenceServiceTest {

    @Mock private DashboardPreferenceRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;

    private DashboardPreferenceService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new DashboardPreferenceService(repository, currentUserProvider);
        userId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(userId, "viewer", Set.of("VIEWER")));
    }

    @Test
    void current_neverConfiguredReturnsFullDefaultSetNotConfigured() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        DashboardPreferenceService.Preferences prefs = service.current();

        assertThat(prefs.configured()).isFalse();
        assertThat(prefs.tiles()).containsExactly(DashboardTile.values());
    }

    @Test
    void current_savedEmptyListIsADeliberateChoiceDistinctFromDefault() {
        DashboardPreference saved = new DashboardPreference();
        saved.setUserId(userId);
        saved.setTiles(List.of());
        when(repository.findByUserId(userId)).thenReturn(Optional.of(saved));

        DashboardPreferenceService.Preferences prefs = service.current();

        assertThat(prefs.configured()).isTrue();
        assertThat(prefs.tiles()).isEmpty();
    }

    @Test
    void save_dedupesPreservingFirstOccurrenceOrder() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        DashboardPreferenceService.Preferences prefs = service.save(List.of(
                DashboardTile.LOW_STOCK, DashboardTile.ASSET_SUMMARY, DashboardTile.LOW_STOCK));

        assertThat(prefs.tiles()).containsExactly(DashboardTile.LOW_STOCK, DashboardTile.ASSET_SUMMARY);
        ArgumentCaptor<DashboardPreference> captor = ArgumentCaptor.forClass(DashboardPreference.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTiles()).containsExactly("LOW_STOCK", "ASSET_SUMMARY");
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void save_updatesExistingRowInsteadOfCreatingASecond() {
        DashboardPreference existing = new DashboardPreference();
        existing.setUserId(userId);
        existing.setTiles(List.of("ASSET_SUMMARY"));
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any(DashboardPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(List.of(DashboardTile.AUDIT_CALENDAR));

        verify(repository).save(existing);
        assertThat(existing.getTiles()).containsExactly("AUDIT_CALENDAR");
        assertThat(existing.getUpdatedBy()).isEqualTo(userId);
    }
}
