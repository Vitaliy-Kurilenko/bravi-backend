package ua.com.bravi.bravi.shared.util;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Reads database constraint metadata out of Spring's data access exceptions.
 * For a partial unique index the database reports the index name.
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    /** Returns the name of the violated constraint, or {@code null} when the database reported none. */
    public static String nameOf(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
