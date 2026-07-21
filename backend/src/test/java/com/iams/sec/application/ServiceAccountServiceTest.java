package com.iams.sec.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.common.security.ServiceAccountPrincipal;
import com.iams.sec.domain.SecurityEventType;
import com.iams.sec.domain.ServiceAccount;
import com.iams.sec.domain.ServiceAccountRepository;
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
class ServiceAccountServiceTest {

    @Mock private ServiceAccountRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;

    private ServiceAccountService service;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ServiceAccountService(repository, currentUserProvider, securityEventLogger);
        lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(actor, "admin", Set.of("SUPER_ADMIN")));
    }

    @Test
    void create_storesOnlyAHashOfTheKey_andReturnsTheRawKeyOnce() {
        when(repository.existsByName("acct-export")).thenReturn(false);

        ServiceAccountService.Issued issued = service.create("acct-export", "Nightly export",
                Set.of("INT_ACCOUNTING_READ"));

        // US-SEC-15: the raw key is returned but NEVER stored raw.
        assertThat(issued.rawApiKey()).startsWith("iamssvc_");
        ArgumentCaptor<ServiceAccount> saved = ArgumentCaptor.forClass(ServiceAccount.class);
        verify(repository).save(saved.capture());
        ServiceAccount account = saved.getValue();
        assertThat(account.getApiKeyHash()).isNotEqualTo(issued.rawApiKey());
        assertThat(account.getApiKeyHash()).hasSize(64); // SHA-256 hex
        assertThat(issued.rawApiKey()).startsWith(account.getApiKeyPrefix()); // prefix is a non-secret slice
        assertThat(account.getScopes()).containsExactly("INT_ACCOUNTING_READ");
        assertThat(account.isActive()).isTrue();
        assertThat(account.getCreatedBy()).isEqualTo(actor);
        verify(securityEventLogger).record(org.mockito.ArgumentMatchers.eq(SecurityEventType.SERVICE_ACCOUNT_CREATED),
                org.mockito.ArgumentMatchers.eq(actor), any(), any(), any());
    }

    @Test
    void authenticate_roundTripsThroughTheHash_toAPrincipalWithScopes() {
        when(repository.existsByName(any())).thenReturn(false);
        ServiceAccountService.Issued issued = service.create("acct", null, Set.of("INT_ACCOUNTING_READ"));
        ArgumentCaptor<ServiceAccount> saved = ArgumentCaptor.forClass(ServiceAccount.class);
        verify(repository).save(saved.capture());
        ServiceAccount account = saved.getValue();
        account.setId(UUID.randomUUID());
        // The service hashes the presented key and looks it up by that hash.
        when(repository.findByApiKeyHash(account.getApiKeyHash())).thenReturn(Optional.of(account));

        Optional<ServiceAccountPrincipal> principal = service.authenticate(issued.rawApiKey());

        assertThat(principal).isPresent();
        assertThat(principal.get().name()).isEqualTo("acct");
        assertThat(principal.get().hasScope("INT_ACCOUNTING_READ")).isTrue();
        assertThat(principal.get().hasScope("INT_SOMETHING_ELSE")).isFalse();
    }

    @Test
    void authenticate_rejectsAnUnknownOrBlankKey() {
        assertThat(service.authenticate("iamssvc_bogus")).isEmpty(); // findByApiKeyHash returns empty by default
        assertThat(service.authenticate("")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();
    }

    @Test
    void authenticate_rejectsARevokedAccount() {
        ServiceAccount account = new ServiceAccount();
        account.setActive(false);
        account.setApiKeyHash("deadbeef");
        account.setScopes(Set.of("INT_ACCOUNTING_READ"));
        when(repository.findByApiKeyHash(any())).thenReturn(Optional.of(account));

        assertThat(service.authenticate("iamssvc_whatever")).isEmpty();
    }

    @Test
    void create_rejectsAnUnknownScope() {
        when(repository.existsByName(any())).thenReturn(false);
        assertThatThrownBy(() -> service.create("x", null, Set.of("INT_MADE_UP")))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("Unknown scope");
    }

    @Test
    void create_rejectsEmptyScopesAndBlankName() {
        assertThatThrownBy(() -> service.create("x", null, Set.of()))
                .isInstanceOf(ValidationFailedException.class).hasMessageContaining("scope");
        assertThatThrownBy(() -> service.create("  ", null, Set.of("INT_ACCOUNTING_READ")))
                .isInstanceOf(ValidationFailedException.class).hasMessageContaining("name");
    }

    @Test
    void create_rejectsADuplicateName() {
        when(repository.existsByName("dup")).thenReturn(true);
        assertThatThrownBy(() -> service.create("dup", null, Set.of("INT_ACCOUNTING_READ")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void revoke_deactivatesAndLogs_orIdempotentlyDoesNothing_or404s() {
        UUID id = UUID.randomUUID();
        ServiceAccount active = new ServiceAccount();
        active.setName("a");
        active.setActive(true);
        when(repository.findById(id)).thenReturn(Optional.of(active));

        service.revoke(id);

        assertThat(active.isActive()).isFalse();
        verify(securityEventLogger).record(org.mockito.ArgumentMatchers.eq(SecurityEventType.SERVICE_ACCOUNT_REVOKED),
                any(), any(), any(), any());

        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.revoke(missing)).isInstanceOf(NotFoundException.class);
    }
}
