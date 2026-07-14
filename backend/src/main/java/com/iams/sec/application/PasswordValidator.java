package com.iams.sec.application;

import com.iams.common.exception.ValidationErrorItem;
import com.iams.common.exception.ValidationFailedException;
import com.iams.sec.domain.PasswordPolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * US-SEC-05: validates a candidate password against the currently configured
 * {@link PasswordPolicy}. Every unmet rule becomes its own ValidationErrorItem
 * (AC-SEC-05-X: "rejected citing the specific unmet rules", plural) rather
 * than stopping at the first failure - a user fixing a length AND a
 * complexity problem should see both at once, not one-at-a-time.
 */
@Component
public class PasswordValidator {

    private final PasswordPolicyService policyService;

    public PasswordValidator(PasswordPolicyService policyService) {
        this.policyService = policyService;
    }

    public void validate(String password) {
        PasswordPolicy policy = policyService.get();
        List<ValidationErrorItem> violations = new ArrayList<>();

        if (password == null || password.length() < policy.getMinLength()) {
            violations.add(new ValidationErrorItem("password",
                    "Must be at least " + policy.getMinLength() + " characters"));
        }
        if (password != null) {
            if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase)) {
                violations.add(new ValidationErrorItem("password", "Must contain at least one uppercase letter"));
            }
            if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase)) {
                violations.add(new ValidationErrorItem("password", "Must contain at least one lowercase letter"));
            }
            if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit)) {
                violations.add(new ValidationErrorItem("password", "Must contain at least one digit"));
            }
            if (policy.isRequireSpecial() && password.chars().allMatch(Character::isLetterOrDigit)) {
                violations.add(new ValidationErrorItem("password", "Must contain at least one special character"));
            }
        }

        if (!violations.isEmpty()) {
            throw new ValidationFailedException(violations);
        }
    }
}
