package ua.com.bravi.bravi.seller.account.api;

import java.util.Optional;

public interface SellerAccountsApi {

    /**
     * Onboards the current user as a seller: provisions a SELLER account with an
     * owner membership (via the access module) and creates the seller profile.
     */
    SellerAccountView onboardCurrentUser(SellerAccountRegistration registration);

    Optional<SellerAccountView> findByAccountId(Long accountId);
}
