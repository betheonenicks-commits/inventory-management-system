package com.iams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// Explicit @EnableJpaRepositories, rather than relying on Boot's implicit
// auto-configured scan: without it, Spring Data failed to resolve
// AssetRepositoryCustomImpl as AssetRepositoryCustom's fragment implementation
// (it fell back to deriving a query from the method name "search" instead,
// which fails with "No property 'search' found for type 'Asset'") - the
// first time this application was ever actually started against a real
// database surfaced this; nothing in the all-Mockito test suite exercises
// real repository-proxy creation.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = "com.iams")
// Scheduling exists for exactly one job so far: the attachment janitor
// (US-PLAT-02). User-facing time-based transitions (LIF/AUD escalation)
// stay pull-triggered by design - see those services' Javadoc.
@EnableScheduling
public class IamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamsApplication.class, args);
    }
}
