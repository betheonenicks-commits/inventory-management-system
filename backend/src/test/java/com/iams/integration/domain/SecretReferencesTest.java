package com.iams.integration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iams.common.exception.ValidationFailedException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * US-SEC-15 (AC-SEC-15-X) is exactly this class's job: an inline secret is refused, a
 * secrets-manager reference is accepted. This is the primitive every integration config
 * leans on, so it is tested directly and thoroughly.
 */
class SecretReferencesTest {

    @Test
    void acceptsEveryRecognisedReferenceScheme() {
        assertThat(SecretReferences.isReference("env:ACCOUNTING_API_KEY")).isTrue();
        assertThat(SecretReferences.isReference("file:/run/secrets/erp_key")).isTrue();
        assertThat(SecretReferences.isReference("vault:secret/data/erp#apiKey")).isTrue();
        assertThat(SecretReferences.isReference("awssm:arn:aws:secretsmanager:us-east-1:123:secret:erp")).isTrue();
        assertThat(SecretReferences.isReference("arn:aws:secretsmanager:us-east-1:123:secret:erp-AbC")).isTrue();
        assertThat(SecretReferences.isReference("secretref:erp-export")).isTrue();
        assertThat(SecretReferences.isReference("  vault:secret/x#k  ")).isTrue(); // trimmed
    }

    @Test
    void rejectsInlinePlaintextSecretsAndJunk() {
        assertThat(SecretReferences.isReference("hunter2")).isFalse();            // a bare password
        assertThat(SecretReferences.isReference("sk_live_abc123DEF456")).isFalse(); // a real-looking API key
        assertThat(SecretReferences.isReference("https://erp.example.com")).isFalse(); // a URL is not a secret ref
        assertThat(SecretReferences.isReference("env:")).isFalse();               // scheme with no target
        assertThat(SecretReferences.isReference("env: has spaces")).isFalse();
        assertThat(SecretReferences.isReference("")).isFalse();
        assertThat(SecretReferences.isReference(null)).isFalse();
    }

    @Test
    void requireReference_throwsOnInlineSecret_andNeverEchoesTheValue() {
        String secret = "sk_live_SUPERSECRET";
        assertThatThrownBy(() -> SecretReferences.requireReference("credentialRef", secret))
                .isInstanceOf(ValidationFailedException.class)
                // AC-SEC-15-H: the plaintext must never appear in the error (it would land in a log)
                .hasMessageNotContaining(secret);
    }

    @Test
    void requireReference_throwsOnBlank() {
        assertThatThrownBy(() -> SecretReferences.requireReference("credentialRef", "  "))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> SecretReferences.requireReference("credentialRef", null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void requireReference_passesForAReference() {
        SecretReferences.requireReference("credentialRef", "vault:secret/erp#key"); // no throw
    }

    @Test
    void rejectInlineSecretsInConfig_flagsASecretSmuggledIntoTheConfigMap() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("baseUrl", "https://erp.example.com"); // non-secret, fine
        config.put("apiPassword", "plaintextpw");          // a secret-named key with an inline value -> reject
        assertThatThrownBy(() -> SecretReferences.rejectInlineSecretsInConfig(config))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageNotContaining("plaintextpw");
    }

    @Test
    void rejectInlineSecretsInConfig_allowsASecretKeyWhoseValueIsAReference() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("baseUrl", "https://erp.example.com");
        config.put("api_key", "env:ERP_API_KEY"); // secret-named but a proper reference -> allowed
        SecretReferences.rejectInlineSecretsInConfig(config); // no throw
    }

    @Test
    void rejectInlineSecretsInConfig_isNoOpForNullOrNonSecretConfig() {
        SecretReferences.rejectInlineSecretsInConfig(null);
        Map<String, String> config = new LinkedHashMap<>();
        config.put("scheduleCron", "0 2 * * *");
        config.put("timeoutSeconds", "30");
        SecretReferences.rejectInlineSecretsInConfig(config); // no throw
    }
}
