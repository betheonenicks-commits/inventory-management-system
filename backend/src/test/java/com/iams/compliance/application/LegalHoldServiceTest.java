package com.iams.compliance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.LegalHold;
import com.iams.compliance.domain.LegalHoldRepository;
import com.iams.compliance.domain.LegalHoldScopeType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegalHoldServiceTest {

    @Mock private LegalHoldRepository holdRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    private LegalHoldService service;
    private UUID scopeId;

    @BeforeEach
    void setUp() {
        service = new LegalHoldService(holdRepository, currentUserProvider);
        scopeId = UUID.randomUUID();
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "compliance1", Set.of("COMPLIANCE_OFFICER")));
    }

    @Test
    void place_rejectsBlankReason() {
        assertThatThrownBy(() -> service.place(LegalHoldScopeType.ASSET, scopeId, " "))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void place_rejectsWhenActiveHoldAlreadyExists() {
        when(holdRepository.findByScopeTypeAndScopeIdAndActiveTrue(LegalHoldScopeType.ASSET, scopeId))
                .thenReturn(Optional.of(new LegalHold()));

        assertThatThrownBy(() -> service.place(LegalHoldScopeType.ASSET, scopeId, "Litigation pending"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void place_succeeds() {
        when(holdRepository.findByScopeTypeAndScopeIdAndActiveTrue(LegalHoldScopeType.AUDIT, scopeId)).thenReturn(Optional.empty());
        when(holdRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        LegalHold result = service.place(LegalHoldScopeType.AUDIT, scopeId, "Litigation pending");

        assertThat(result.isActive()).isTrue();
        assertThat(result.getScopeType()).isEqualTo(LegalHoldScopeType.AUDIT);
    }

    @Test
    void lift_rejectsAlreadyLifted() {
        LegalHold hold = new LegalHold();
        hold.setId(UUID.randomUUID());
        hold.setActive(false);
        when(holdRepository.findById(hold.getId())).thenReturn(Optional.of(hold));

        assertThatThrownBy(() -> service.lift(hold.getId(), "Investigation closed")).isInstanceOf(ConflictException.class);
    }

    @Test
    void lift_succeeds() {
        LegalHold hold = new LegalHold();
        hold.setId(UUID.randomUUID());
        hold.setActive(true);
        when(holdRepository.findById(hold.getId())).thenReturn(Optional.of(hold));
        when(holdRepository.saveAndFlush(hold)).thenReturn(hold);

        LegalHold result = service.lift(hold.getId(), "Investigation closed");

        assertThat(result.isActive()).isFalse();
        assertThat(result.getLiftReason()).isEqualTo("Investigation closed");
    }

    @Test
    void requireNoActiveHold_throws_whenHoldActive() {
        when(holdRepository.findByScopeTypeAndScopeIdAndActiveTrue(LegalHoldScopeType.ASSET, scopeId))
                .thenReturn(Optional.of(new LegalHold()));

        assertThatThrownBy(() -> service.requireNoActiveHold(LegalHoldScopeType.ASSET, scopeId))
                .isInstanceOf(LegalHoldActiveException.class);
    }

    @Test
    void requireNoActiveHold_passesSilently_whenNoActiveHold() {
        when(holdRepository.findByScopeTypeAndScopeIdAndActiveTrue(LegalHoldScopeType.ASSET, scopeId)).thenReturn(Optional.empty());

        service.requireNoActiveHold(LegalHoldScopeType.ASSET, scopeId);
    }
}
