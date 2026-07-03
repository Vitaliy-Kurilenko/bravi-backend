package ua.com.bravi.bravi.seller.stores.api;

/** Minimal store data captured during onboarding (a DRAFT store; other fields get defaults). */
public record StoreDraft(
        String name,
        String description,
        String logoUrl
) {
}
