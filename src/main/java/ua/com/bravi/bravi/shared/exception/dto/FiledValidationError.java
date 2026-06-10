package ua.com.bravi.bravi.shared.exception.dto;

public record FiledValidationError (
        String field,
        String message
) { }
