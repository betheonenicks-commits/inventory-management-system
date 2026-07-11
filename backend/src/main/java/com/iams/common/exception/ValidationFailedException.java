package com.iams.common.exception;

import java.util.List;

/**
 * Raised whenever request data fails validation that JSR-380 cannot express -
 * chiefly dynamic per-category custom-field validation (see CustomFieldValidationService).
 */
public class ValidationFailedException extends RuntimeException {

    private final List<ValidationErrorItem> errors;

    public ValidationFailedException(List<ValidationErrorItem> errors) {
        super("Validation failed: " + errors);
        this.errors = errors;
    }

    public static ValidationFailedException singleField(String field, String message) {
        return new ValidationFailedException(List.of(new ValidationErrorItem(field, message)));
    }

    public List<ValidationErrorItem> getErrors() {
        return errors;
    }
}
