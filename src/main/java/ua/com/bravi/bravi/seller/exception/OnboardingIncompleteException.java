package ua.com.bravi.bravi.seller.exception;

import java.util.List;

/** Thrown when onboarding completion is attempted while required steps are still missing. */
public class OnboardingIncompleteException extends RuntimeException {

    private final List<String> missing;

    public OnboardingIncompleteException(List<String> missing) {
        super("Onboarding is incomplete: " + String.join(", ", missing));
        this.missing = List.copyOf(missing);
    }

    public List<String> getMissing() {
        return missing;
    }
}
