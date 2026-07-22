package com.iams.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.iams.platform.api.dto.SystemHealthResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    @Mock private HealthEndpoint healthEndpoint;
    @Mock private CompositeHealth composite;

    @Test
    void check_flattensCompositeComponents() {
        Map<String, HealthComponent> parts = Map.of(
                "db", Health.up().build(),
                "diskSpace", Health.status(Status.DOWN).build());
        when(composite.getStatus()).thenReturn(Status.UP);
        when(composite.getComponents()).thenReturn(parts);
        when(healthEndpoint.health()).thenReturn(composite);

        SystemHealthResponse result = new SystemHealthService(healthEndpoint).check();

        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.components()).containsEntry("db", "UP").containsEntry("diskSpace", "DOWN");
        assertThat(result.checkedAt()).isNotNull();
    }

    @Test
    void check_handlesNonCompositeHealth() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        SystemHealthResponse result = new SystemHealthService(healthEndpoint).check();

        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.components()).isEmpty();
    }
}
