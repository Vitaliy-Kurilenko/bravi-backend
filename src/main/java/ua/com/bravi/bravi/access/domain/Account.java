package ua.com.bravi.bravi.access.domain;

public record Account(
        Long id,
        String publicId,
        AccountType type,
        AccountStatus status
) {
}
