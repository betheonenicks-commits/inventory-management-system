package com.iams.compliance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.compliance.domain.RetentionEntityType;
import com.iams.compliance.domain.RetentionExpiryAction;
import com.iams.compliance.domain.RetentionPolicy;
import com.iams.compliance.domain.RetentionPolicyRepository;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventLogRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetentionPolicyServiceTest {

    @Mock private RetentionPolicyRepository policyRepository;
    @Mock private SecurityEventLogRepository securityEventLogRepository;
    @Mock private SecurityEventLogger securityEventLogger;
    @Mock private CurrentUserProvider currentUserProvider;

    private RetentionPolicyService service;

    @BeforeEach
    void setUp() {
        service = new RetentionPolicyService(policyRepository, securityEventLogRepository, securityEventLogger, currentUserProvider);
        org.mockito.Mockito.lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(UUID.randomUUID(), "compliance1", Set.of("COMPLIANCE_OFFICER")));
    }

    @Test
    void save_rejectsBelowFloor_forSecurityEventLog() {
        assertThatThrownBy(() -> service.save(RetentionEntityType.SECURITY_EVENT_LOG, 365, RetentionExpiryAction.DELETE))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("2555");
    }

    @Test
    void save_rejectsBelowFloor_forDisposedAsset() {
        assertThatThrownBy(() -> service.save(RetentionEntityType.DISPOSED_ASSET, 30, RetentionExpiryAction.DELETE))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void save_acceptsAtOrAboveFloor() {
        when(policyRepository.findByEntityType(RetentionEntityType.SECURITY_EVENT_LOG)).thenReturn(Optional.empty());
        when(policyRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        RetentionPolicy result = service.save(RetentionEntityType.SECURITY_EVENT_LOG, 2555, RetentionExpiryAction.DELETE);

        assertThat(result.getRetentionPeriodDays()).isEqualTo(2555);
    }

    @Test
    void save_hasNoFloor_forEntityTypesNotNamedInBrd() {
        when(policyRepository.findByEntityType(RetentionEntityType.PERSON)).thenReturn(Optional.empty());
        when(policyRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        RetentionPolicy result = service.save(RetentionEntityType.PERSON, 30, RetentionExpiryAction.ANONYMIZE);

        assertThat(result.getRetentionPeriodDays()).isEqualTo(30);
    }

    @Test
    void runPurge_throws_whenNoPolicyConfigured() {
        when(policyRepository.findByEntityType(RetentionEntityType.SECURITY_EVENT_LOG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runPurge()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runPurge_deletesRowsOlderThanCutoffAndLogsTheRun() {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setEntityType(RetentionEntityType.SECURITY_EVENT_LOG);
        policy.setRetentionPeriodDays(2555);
        policy.setExpiryAction(RetentionExpiryAction.DELETE);
        when(policyRepository.findByEntityType(RetentionEntityType.SECURITY_EVENT_LOG)).thenReturn(Optional.of(policy));
        when(securityEventLogRepository.deleteByCreatedAtBefore(org.mockito.ArgumentMatchers.any())).thenReturn(42L);

        long deleted = service.runPurge();

        assertThat(deleted).isEqualTo(42L);
        org.mockito.Mockito.verify(securityEventLogger).record(
                org.mockito.ArgumentMatchers.eq(com.iams.sec.domain.SecurityEventType.RETENTION_PURGE_EXECUTED),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.contains("42"));
    }

    @Test
    void runPurge_refusesWhenExpiryActionIsNotDelete() {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setEntityType(RetentionEntityType.SECURITY_EVENT_LOG);
        policy.setRetentionPeriodDays(2555);
        policy.setExpiryAction(RetentionExpiryAction.HOLD_ELIGIBLE);
        when(policyRepository.findByEntityType(RetentionEntityType.SECURITY_EVENT_LOG)).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> service.runPurge()).isInstanceOf(ValidationFailedException.class);
    }
}
