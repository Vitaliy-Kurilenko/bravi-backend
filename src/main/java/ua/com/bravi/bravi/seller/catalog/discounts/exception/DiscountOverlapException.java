package ua.com.bravi.bravi.seller.catalog.discounts.exception;

import lombok.Getter;

import java.time.Instant;

/**
 * A product would end up with two discounts in effect at the same time. Both sides of the collision
 * are reported so the client can point at the two rows that disagree.
 */
@Getter
public class DiscountOverlapException extends RuntimeException {

    /**
     * One side of a collision. {@code index} is the position in the submitted array and is null for a
     * stored row that was not resubmitted; {@code publicId} is null for a row that does not exist yet.
     */
    public record Side(Integer index, String publicId, Instant startsAt, Instant endsAt) {
    }

    private final String field;
    private final Side submitted;
    private final Side conflicting;

    public DiscountOverlapException(String field, String message, Side submitted, Side conflicting) {
        super(message);
        this.field = field;
        this.submitted = submitted;
        this.conflicting = conflicting;
    }
}
