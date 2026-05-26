package ua.com.bravi.bravi.exception.dto;

public record FiledValidationError (
        String field,
        String message
) { }
