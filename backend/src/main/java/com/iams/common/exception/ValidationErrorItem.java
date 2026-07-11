package com.iams.common.exception;

/**
 * A single field-level validation failure, per the API spec's {field, message} convention.
 */
public record ValidationErrorItem(String field, String message) {
}
