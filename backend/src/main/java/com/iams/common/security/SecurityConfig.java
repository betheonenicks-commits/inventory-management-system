package com.iams.common.security;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(DevSecurityProperties.class)
public class SecurityConfig {

    /**
     * US-SEC-14: the ONLY endpoints an integration service account may reach. Everything
     * else is default-denied for a {@link ServiceAccountPrincipal} at the web layer -
     * crucially including endpoints that carry no {@code @perm.has} of their own (e.g. the
     * read side of AssetController), which a merely-authenticated principal would otherwise
     * pass. Method-level {@code @svc.hasScope} then verifies the account holds the exact
     * scope this endpoint needs, so this list gates reachability, not the scope grant.
     */
    private static final List<RequestMatcher> SERVICE_ACCOUNT_ENDPOINTS = List.of(
            new AntPathRequestMatcher("/api/v1/reports/depreciation", "GET"));

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            ServiceAccountAuthenticationFilter serviceAccountAuthenticationFilter,
                                            JsonAuthenticationEntryPoint authenticationEntryPoint,
                                            JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // pure bearer-token API, no cookie auth in this phase
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/unlock/request",
                                "/api/v1/auth/unlock/confirm",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().access(authenticatedButIntegrationsDefaultDeny()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // US-SEC-14: X-Api-Key (service accounts) is resolved before the human JWT
                // filter; both leave the context empty on failure, so the same 401 applies.
                // The JWT filter is registered first so its class becomes a valid ordering
                // reference (a custom filter has no built-in order until it's been added).
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(serviceAccountAuthenticationFilter, JwtAuthenticationFilter.class)
                .build();
    }

    /**
     * Replaces a plain {@code .anyRequest().authenticated()}: an authenticated human is
     * allowed as before, but a US-SEC-14 service account is allowed ONLY on the whitelisted
     * integration endpoints and denied (403) everywhere else - so a new unguarded endpoint
     * can never silently become reachable by an integration. Anonymous is still denied (401).
     */
    private static AuthorizationManager<RequestAuthorizationContext> authenticatedButIntegrationsDefaultDeny() {
        return (authentication, context) -> {
            Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return new AuthorizationDecision(false);
            }
            if (auth.getPrincipal() instanceof ServiceAccountPrincipal) {
                boolean reachable = SERVICE_ACCOUNT_ENDPOINTS.stream().anyMatch(m -> m.matches(context.getRequest()));
                return new AuthorizationDecision(reachable);
            }
            return new AuthorizationDecision(true);
        };
    }
}
