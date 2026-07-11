package com.iams.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates the dev-stub HS256 JWT. Deliberately simpler than the
 * ratified production design (RS256, refresh-token cookie, LDAP/SSO) - this is
 * the seam EPIC-USR/SEC replaces, not the contract other code depends on
 * (CurrentUserProvider is the contract).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(DevSecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.getJwt().getExpirationMinutes();
    }

    public String issue(CurrentUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.id().toString())
                .claim("username", user.username())
                .claim("roles", List.copyOf(user.roles()))
                .issuer("iams-dev")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    /**
     * Returns the CurrentUser encoded in a valid token, or empty if the token
     * is missing, malformed, expired, or has a bad signature. Never throws -
     * callers (JwtAuthenticationFilter) must not have to distinguish failure
     * modes, since an unauthenticated request should look the same regardless.
     */
    public Optional<CurrentUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            Set<String> roleSet = roles == null ? new HashSet<>() : new HashSet<>(roles);
            return Optional.of(new CurrentUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get("username", String.class),
                    roleSet
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
