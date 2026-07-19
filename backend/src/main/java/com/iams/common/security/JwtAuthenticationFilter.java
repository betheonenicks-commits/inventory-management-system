package com.iams.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the Authorization: Bearer <jwt> header, if present. Deliberately
 * never throws on an invalid/missing token - it just leaves the SecurityContext
 * empty, so JsonAuthenticationEntryPoint (not this filter) produces the 401 in
 * the correct problem+json shape via Spring Security's normal exception
 * translation. Throwing here would bypass that translation.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AccessRevocationCheck accessRevocationCheck;

    public JwtAuthenticationFilter(JwtService jwtService, AccessRevocationCheck accessRevocationCheck) {
        this.jwtService = jwtService;
        this.accessRevocationCheck = accessRevocationCheck;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            Optional<CurrentUser> user = jwtService.parse(token);
            // US-USR-08: a structurally-valid, unexpired token is still refused
            // if its user has since been deactivated - the SecurityContext is
            // left empty exactly as for a bad token, so the entry point produces
            // the normal 401.
            if (user.isPresent() && accessRevocationCheck.isRevoked(user.get().id())) {
                chain.doFilter(request, response);
                return;
            }
            if (user.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                var authorities = user.get().roles().stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                var authToken = new UsernamePasswordAuthenticationToken(user.get(), null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}
