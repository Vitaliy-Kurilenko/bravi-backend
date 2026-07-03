package ua.com.bravi.bravi.access.api;

/**
 * Read model of an account. Type/status exposed as String to keep the
 * access domain enums module-internal (mirrors identity's CurrentUserView).
 */
public record AccountView(
        Long id,
        String publicId,
        String type,
        String status
) {
}
