package com.iams.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.analytics.domain.UsageEvent;
import com.iams.analytics.domain.UsageEventRepository;
import com.iams.common.security.CurrentUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageRecorderTest {

    @Mock private UsageEventRepository repository;

    private UsageRecorder recorder;
    private UUID userId;

    @BeforeEach
    void setUp() {
        recorder = new UsageRecorder(repository);
        userId = UUID.randomUUID();
    }

    @Test
    void record_writesOneRowPerRoleHeld() {
        recorder.record(new CurrentUser(userId, "im", Set.of("INVENTORY_MANAGER", "AUDITOR"), Set.of()),
                "assets", "register");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UsageEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<UsageEvent> events = captor.getValue();
        assertThat(events).hasSize(2);
        assertThat(events).extracting(UsageEvent::getRole)
                .containsExactlyInAnyOrder("INVENTORY_MANAGER", "AUDITOR");
        assertThat(events).allSatisfy(e -> {
            assertThat(e.getModule()).isEqualTo("assets");
            assertThat(e.getAction()).isEqualTo("register");
            assertThat(e.getUserId()).isEqualTo(userId);
        });
    }

    @Test
    void record_roleLessPrincipalStillRecordsUnderNone() {
        recorder.record(new CurrentUser(userId, "odd", Set.of(), Set.of()), "search", "global-search");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UsageEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(e -> assertThat(e.getRole()).isEqualTo("NONE"));
    }

    @Test
    void record_neverThrows_whenTheStoreIsDown() {
        when(repository.saveAll(anyList())).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> recorder.record(new CurrentUser(userId, "im", Set.of("AUDITOR"), Set.of()),
                "audits", "scan")).doesNotThrowAnyException();
    }
}
