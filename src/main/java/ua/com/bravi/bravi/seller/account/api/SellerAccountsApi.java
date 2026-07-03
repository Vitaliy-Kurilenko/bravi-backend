package ua.com.bravi.bravi.seller.account.api;

import java.util.Optional;

public interface SellerAccountsApi {

    Optional<SellerAccountView> findByAccountId(Long accountId);
}
