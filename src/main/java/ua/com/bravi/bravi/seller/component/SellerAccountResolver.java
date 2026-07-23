package ua.com.bravi.bravi.seller.component;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.access.api.AccessContextView;
import ua.com.bravi.bravi.access.api.AccountContext;

/**
 * Resolves the internal seller account id for an account-scoped request. The account-context
 * interceptor has already populated {@link AccountContext} from the {@code X-Account-Id} header
 * (validating ACTIVE membership); this guard additionally verifies the account is a SELLER account,
 * yielding 403 otherwise.
 */
@Component
@RequiredArgsConstructor
public class SellerAccountResolver {

    private static final String SELLER = "SELLER";

    private final AccountContext accountContext;

    public Long resolveSellerAccountId() {
        AccessContextView context = accountContext.getContext()
                .orElseThrow(() -> new AccessDeniedException("No access to this account"));
        if (!SELLER.equals(context.accountType())) {
            throw new AccessDeniedException("Not a seller account");
        }
        return context.accountId();
    }
}
