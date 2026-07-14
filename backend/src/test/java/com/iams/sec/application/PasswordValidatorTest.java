package com.iams.sec.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.iams.common.exception.ValidationFailedException;
import com.iams.sec.domain.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock private PasswordPolicyService policyService;

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator(policyService);
    }

    private PasswordPolicy policy(int minLength, boolean upper, boolean lower, boolean digit, boolean special) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setMinLength(minLength);
        policy.setRequireUppercase(upper);
        policy.setRequireLowercase(lower);
        policy.setRequireDigit(digit);
        policy.setRequireSpecial(special);
        return policy;
    }

    @Test
    void validate_accepts_compliantPassword() {
        when(policyService.get()).thenReturn(policy(8, false, false, false, false));
        validator.validate("password123");
        // no exception - success
    }

    @Test
    void validate_rejectsTooShort() {
        when(policyService.get()).thenReturn(policy(12, false, false, false, false));

        assertThatThrownBy(() -> validator.validate("short1"))
                .isInstanceOfSatisfying(ValidationFailedException.class,
                        ex -> assertThat(ex.getErrors()).hasSize(1));
    }

    @Test
    void validate_citesEveryUnmetRule_notJustTheFirst() {
        when(policyService.get()).thenReturn(policy(12, true, true, true, true));

        assertThatThrownBy(() -> validator.validate("short"))
                .isInstanceOfSatisfying(ValidationFailedException.class, ex -> {
                    // too short, no uppercase, no digit, no special (has lowercase) - 4 violations
                    assertThat(ex.getErrors()).hasSize(4);
                });
    }

    @Test
    void validate_rejectsMissingComplexity() {
        when(policyService.get()).thenReturn(policy(4, true, true, true, true));

        assertThatThrownBy(() -> validator.validate("alllowercase"))
                .isInstanceOfSatisfying(ValidationFailedException.class, ex -> {
                    // no uppercase, no digit, no special - 3 violations
                    assertThat(ex.getErrors()).hasSize(3);
                });
    }

    @Test
    void validate_acceptsFullyCompliantComplexPassword() {
        when(policyService.get()).thenReturn(policy(8, true, true, true, true));
        validator.validate("Str0ng!Pass");
        // no exception - success
    }
}
