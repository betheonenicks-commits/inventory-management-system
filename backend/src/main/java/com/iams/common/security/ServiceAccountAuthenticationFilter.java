package com.iams.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * US-SEC-14: authenticates an integration by the {@code X-Api-Key} header, distinct
 * from the human {@code Authorization: Bearer <jwt>} header {@link JwtAuthenticationFilter}
 * handles - the two mechanisms never collide. Like the JWT filter it NEVER throws on a
 * bad/missing key: it just leaves the SecurityContext empty, so JsonAuthenticationEntryPoint
 * produces the standard 401. A successful key sets a {@link ServiceAccountPrincipal} whose
 * only authorities are its scopes (SCOPE_*), so it clears the authenticated gate but is
 * refused every human endpoint (@perm.has) and may reach only @svc.hasScope endpoints.
 */
@Component
public class ServiceAccountAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";

    private final ServiceAccountAuthenticator authenticator;

    public ServiceAccountAuthenticationFilter(ServiceAccountAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey != null && !apiKey.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticator.authenticate(apiKey).ifPresent(principal -> {
                List<GrantedAuthority> authorities = principal.scopes().stream()
                        .map(scope -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + scope))
                        .toList();
                var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            });
        }
        chain.doFilter(request, response);
    }
}
