package com.iams.asset.application;

import com.iams.asset.domain.AssetCustomFieldDefinition;
import com.iams.common.exception.ValidationErrorItem;
import com.iams.common.exception.ValidationFailedException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Validates the dynamic asset.custom_attributes JSONB payload against a
 * category's AssetCustomFieldDefinition rows (FR-AST-06). This is hand-rolled
 * rather than JSR-380 because the shape is dynamic per category - there is no
 * static DTO field to annotate.
 */
@Service
public class CustomFieldValidationService {

    /**
     * Validates the submitted custom field values against the category's
     * definitions. Throws ValidationFailedException naming every offending
     * field (not just the first) if anything is wrong - per AC-AST-01-X /
     * AC-AST-06-X, the response must name the field.
     */
    public void validate(List<AssetCustomFieldDefinition> definitions, Map<String, Object> submitted) {
        List<ValidationErrorItem> errors = new ArrayList<>();
        Map<String, Object> values = submitted == null ? Map.of() : submitted;

        for (AssetCustomFieldDefinition def : definitions) {
            String path = "customFields." + def.getFieldKey();
            Object value = values.get(def.getFieldKey());
            boolean blank = value == null || (value instanceof String s && s.isBlank());

            if (def.isRequired() && blank) {
                errors.add(new ValidationErrorItem(path, "This field is required"));
                continue;
            }
            if (blank) {
                continue;
            }

            switch (def.getDataType()) {
                case TEXT -> {
                    if (!(value instanceof String)) {
                        errors.add(new ValidationErrorItem(path, "Must be text"));
                    }
                }
                case NUMBER -> {
                    if (!(value instanceof Number) && !isNumericString(value)) {
                        errors.add(new ValidationErrorItem(path, "Must be a number"));
                    }
                }
                case BOOLEAN -> {
                    if (!(value instanceof Boolean)) {
                        errors.add(new ValidationErrorItem(path, "Must be true or false"));
                    }
                }
                case DATE -> {
                    if (!(value instanceof String dateStr) || !isValidDate(dateStr)) {
                        errors.add(new ValidationErrorItem(path, "Must be a valid date (YYYY-MM-DD)"));
                    }
                }
                case ENUM -> {
                    List<String> options = def.getEnumOptions();
                    if (!(value instanceof String s) || options == null || !options.contains(s)) {
                        errors.add(new ValidationErrorItem(path, "Must be one of: " + def.getEnumOptions()));
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private boolean isNumericString(Object value) {
        if (!(value instanceof String s)) {
            return false;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
