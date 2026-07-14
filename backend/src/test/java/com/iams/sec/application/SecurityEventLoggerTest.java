package com.iams.sec.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.iams.sec.domain.SecurityEventLog;
import com.iams.sec.domain.SecurityEventLogRepository;
import com.iams.sec.domain.SecurityEventType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityEventLoggerTest {

    @Mock private SecurityEventLogRepository repository;

    private SecurityEventLogger logger;

    @BeforeEach
    void setUp() {
        logger = new SecurityEventLogger(repository);
    }

    @Test
    void record_savesEveryField() {
        UUID actorId = UUID.randomUUID();
        logger.record(SecurityEventType.LOGIN_FAILURE, actorId, "dnraike", "127.0.0.1", "bad password");

        ArgumentCaptor<SecurityEventLog> captor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(repository).save(captor.capture());
        SecurityEventLog saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(SecurityEventType.LOGIN_FAILURE);
        assertThat(saved.getActorUserId()).isEqualTo(actorId);
        assertThat(saved.getUsernameAttempted()).isEqualTo("dnraike");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getDetail()).isEqualTo("bad password");
    }

    @Test
    void record_allowsNullActor_forUnknownUsernameFailures() {
        logger.record(SecurityEventType.LOGIN_FAILURE, null, "unknownuser", "127.0.0.1", null);

        ArgumentCaptor<SecurityEventLog> captor = ArgumentCaptor.forClass(SecurityEventLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isNull();
        assertThat(captor.getValue().getUsernameAttempted()).isEqualTo("unknownuser");
    }
}
