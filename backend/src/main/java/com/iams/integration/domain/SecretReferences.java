package com.iams.integration.domain;

import com.iams.common.exception.ValidationFailedException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * US-SEC-15 (AC-SEC-15-H / AC-SEC-15-X): the one rule that makes "never store integration
 * credentials in plaintext" enforceable. A credential must be a <em>secrets-manager
 * reference</em> - a pointer the runtime resolves at use time - never the secret value
 * itself. Anything that is not a recognised reference is rejected up front (400
 * VALIDATION_FAILED), so a plaintext secret never reaches the database, a Compose file,
 * or a log line.
 *
 * <p>Recognised reference schemes (on-premises-appropriate, per FR-SEC-13's on-prem model):
 * <ul>
 *   <li>{@code env:NAME} - an environment variable resolved at runtime</li>
 *   <li>{@code file:/run/secrets/name} - a mounted secret file (Docker/K8s secrets)</li>
 *   <li>{@code vault:path/to/secret#key} - a HashiCorp Vault path</li>
 *   <li>{@code awssm:arn:aws:secretsmanager:...} or a bare {@code arn:aws:secretsmanager:...} - AWS Secrets Manager</li>
 *   <li>{@code secretref:handle} - a generic secrets-manager handle</li>
 * </ul>
 *
 * <p>The rejection message deliberately never echoes the submitted value - echoing it would
 * itself write the plaintext to a log, defeating AC-SEC-15-H.
 */
public final class SecretReferences {

    private static final Pattern REFERENCE = Pattern.compile(
            "^(env:\\S+"
            + "|file:\\S+"
            + "|vault:\\S+"
            + "|awssm:\\S+"
            + "|secretref:\\S+"
            + "|arn:aws:secretsmanager:\\S+)$");

    /**
     * Config keys that name a secret - a value under one of these must itself be a reference,
     * so a secret can't be smuggled into the generic (non-secret) config map as plaintext.
     */
    private static final Pattern SECRET_KEY = Pattern.compile(
            "(?i).*(password|passwd|secret|token|api[_-]?key|credential|private[_-]?key|access[_-]?key).*");

    private SecretReferences() {
    }

    /** True if {@code value} is a recognised secrets-manager reference (not an inline secret). */
    public static boolean isReference(String value) {
        return value != null && REFERENCE.matcher(value.trim()).matches();
    }

    /**
     * Require {@code value} to be a secrets-manager reference; throw 400 VALIDATION_FAILED if
     * it is blank or looks like an inline secret. The message never contains {@code value}.
     */
    public static void requireReference(String field, String value) {
        if (value == null || value.isBlank()) {
            throw ValidationFailedException.singleField(field, "A secrets-manager reference is required");
        }
        if (!isReference(value)) {
            throw ValidationFailedException.singleField(field,
                    "must be a secrets-manager reference (e.g. env:NAME, vault:path#key, awssm:arn:..., file:/run/secrets/x), "
                    + "not an inline secret");
        }
    }

    /**
     * AC-SEC-15-X for the generic config map: any entry whose key names a secret must carry a
     * reference, not a plaintext value. Non-secret entries (a URL, a schedule) are untouched.
     */
    public static void rejectInlineSecretsInConfig(Map<String, String> config) {
        if (config == null) {
            return;
        }
        for (Map.Entry<String, String> entry : config.entrySet()) {
            if (SECRET_KEY.matcher(entry.getKey()).matches() && !isReference(entry.getValue())) {
                throw ValidationFailedException.singleField("config." + entry.getKey(),
                        "names a secret, so its value must be a secrets-manager reference, not an inline secret");
            }
        }
    }
}
