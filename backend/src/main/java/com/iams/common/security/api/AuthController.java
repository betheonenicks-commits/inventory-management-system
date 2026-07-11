package com.iams.common.security.api;

import com.iams.common.security.CurrentUser;
import com.iams.common.security.DevSecurityProperties;
import com.iams.common.security.InvalidCredentialsException;
import com.iams.common.security.JwtService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-mode auth stub: one hardcoded user, real JWT issuance/validation. This is
 * the seam EPIC-USR/SEC (real LDAP/SSO/RBAC) replaces - CurrentUserProvider and
 * the token contract are what the rest of the system depends on, not this class.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final DevSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String encodedDevPassword;

    public AuthController(DevSecurityProperties properties, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Encoding the configured plaintext dev password once at startup keeps the
        // comparison path (matches()) realistic without needing a user store yet.
        this.encodedDevPassword = passwordEncoder.encode(properties.getDevUser().getPassword());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        DevSecurityProperties.DevUser devUser = properties.getDevUser();
        boolean usernameMatches = devUser.getUsername().equals(request.username());
        boolean passwordMatches = passwordEncoder.matches(request.password(), encodedDevPassword);
        if (!usernameMatches || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        CurrentUser user = new CurrentUser(devUser.getId(), devUser.getUsername(), Set.of(devUser.getRole()));
        String token = jwtService.issue(user);
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds());
    }
}
