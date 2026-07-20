package ua.com.bravi.bravi.seller.stores.api;

/**
 * Minimal store data captured during onboarding (a DRAFT store; other fields get defaults).
 * The logo is uploaded separately via the presigned-URL flow, not carried here.
 */
public record StoreDraft(
        String name,
        String description,
        String country
) {
}
