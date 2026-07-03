package ua.com.bravi.bravi.seller.account.api;

import java.util.Optional;

public interface SellerAccountsApi {

    Optional<SellerAccountView> findByAccountId(Long accountId);

    /** Sets the onboarding status (NOT_STARTED / IN_PROGRESS / COMPLETED) of a seller account. */
    void updateOnboardingStatus(Long accountId, String onboardingStatus);
}
