package com.iams.integration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.integration.domain.Integration;
import com.iams.integration.domain.IntegrationRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import java.util.LinkedHashMap;
import java.util.Map;
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
class IntegrationServiceTest {

    @Mock private IntegrationRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private SecurityEventLogger securityEventLogger;

    private IntegrationService service;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new IntegrationService(repository, currentUserProvider, securityEventLogger);
        lenient().when(currentUserProvider.current()).thenReturn(new CurrentUser(actor, "sec-officer", Set.of("IT_SECURITY_OFFICER")));
    }

    @Test
    void create_storesAReference_disabledByDefault_andLogs() {
        when(repository.existsByName("Nightly ERP export")).thenReturn(false);

        Map<String, String> config = new LinkedHashMap<>();
        config.put("baseUrl", "https://erp.example.com");
        Integration created = service.create("Nightly ERP export", "ACCOUNTING_EXPORT", "depreciation pull",
                "vault:secret/erp#apiKey", config);

        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(repository).save(saved.capture());
        Integration i = saved.getValue();
        assertThat(i.getCredentialRef()).isEqualTo("vault:secret/erp#apiKey"); // a reference, not a secret
        assertThat(i.isEnabled()).isFalse();                                   // FR-INT-05: disabled by default
        assertThat(i.getCreatedBy()).isEqualTo(actor);
        assertThat(created).isSameAs(i);
        verify(securityEventLogger).record(eq(SecurityEventType.INTEGRATION_CREATED), eq(actor), any(), any(), any());
    }

    @Test
    void create_rejectsAnInlinePlaintextCredential() {
        when(repository.existsByName(any())).thenReturn(false);
        assertThatThrownBy(() -> service.create("x", "ACCOUNTING_EXPORT", null, "sk_live_PLAINTEXT", null))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageNotContaining("sk_live_PLAINTEXT"); // AC-SEC-15-X + never echo the secret
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsASecretSmuggledIntoConfig() {
        when(repository.existsByName(any())).thenReturn(false);
        Map<String, String> config = new LinkedHashMap<>();
        config.put("password", "hunter2");
        assertThatThrownBy(() -> service.create("x", "OUTBOUND_WEBHOOK", null, "env:HOOK_SECRET", config))
                .isInstanceOf(ValidationFailedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_requiresACredential_whenTheTypeNeedsOne() {
        when(repository.existsByName(any())).thenReturn(false);
        assertThatThrownBy(() -> service.create("x", "ACCOUNTING_EXPORT", null, null, null))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("reference is required");
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownTypeAndBlankName() {
        assertThatThrownBy(() -> service.create("x", "MADE_UP", null, "env:X", null))
                .isInstanceOf(ValidationFailedException.class).hasMessageContaining("type");
        assertThatThrownBy(() -> service.create("  ", "ACCOUNTING_EXPORT", null, "env:X", null))
                .isInstanceOf(ValidationFailedException.class).hasMessageContaining("name");
    }

    @Test
    void create_rejectsDuplicateName() {
        when(repository.existsByName("dup")).thenReturn(true);
        assertThatThrownBy(() -> service.create("dup", "ACCOUNTING_EXPORT", null, "env:X", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void setEnabled_flipsAndLogs_thenIsIdempotent() {
        UUID id = UUID.randomUUID();
        Integration integration = new Integration();
        integration.setName("erp");
        integration.setEnabled(false);
        when(repository.findById(id)).thenReturn(Optional.of(integration));

        service.setEnabled(id, true);
        assertThat(integration.isEnabled()).isTrue();
        verify(securityEventLogger).record(eq(SecurityEventType.INTEGRATION_ENABLED), eq(actor), any(), any(), any());

        // idempotent: enabling an already-enabled integration changes nothing and logs nothing more
        service.setEnabled(id, true);
        verify(securityEventLogger).record(eq(SecurityEventType.INTEGRATION_ENABLED), eq(actor), any(), any(), any());
    }

    @Test
    void delete_removesAndLogs_or404s() {
        UUID id = UUID.randomUUID();
        Integration integration = new Integration();
        integration.setName("erp");
        when(repository.findById(id)).thenReturn(Optional.of(integration));

        service.delete(id);
        verify(repository).delete(integration);
        verify(securityEventLogger).record(eq(SecurityEventType.INTEGRATION_DELETED), eq(actor), any(), any(), any());

        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(missing)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_404sWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }
}
