package com.iams.common.security;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * devUser is no longer a live login-check stub (AuthController now checks
 * app_user in the database) - it's the seed values BootstrapUserSeeder uses
 * to create the first real Super Administrator row at startup. Kept under
 * the same "iams.security.dev-user" config key so an operator's existing
 * environment variables / compose overrides keep working unchanged. jwt is
 * still live config, used by JwtService.
 */
@ConfigurationProperties(prefix = "iams.security")
public class DevSecurityProperties {

    private DevUser devUser = new DevUser();
    private Jwt jwt = new Jwt();

    public DevUser getDevUser() {
        return devUser;
    }

    public void setDevUser(DevUser devUser) {
        this.devUser = devUser;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public static class DevUser {
        private UUID id;
        private String username;
        private String password;
        private String displayName;
        private String role;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class Jwt {
        private String secret;
        private long expirationMinutes;
        // US-SEC-01: how long a refresh token stays valid if never used.
        private long refreshExpirationMinutes = 43200; // 30 days
        // US-SEC-06: a refresh token unused for longer than this is treated as an
        // idle session and refused, even if not yet at its own expiry.
        private long refreshIdleTimeoutMinutes = 1440; // 24 hours

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(long expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }

        public long getRefreshExpirationMinutes() {
            return refreshExpirationMinutes;
        }

        public void setRefreshExpirationMinutes(long refreshExpirationMinutes) {
            this.refreshExpirationMinutes = refreshExpirationMinutes;
        }

        public long getRefreshIdleTimeoutMinutes() {
            return refreshIdleTimeoutMinutes;
        }

        public void setRefreshIdleTimeoutMinutes(long refreshIdleTimeoutMinutes) {
            this.refreshIdleTimeoutMinutes = refreshIdleTimeoutMinutes;
        }
    }
}
