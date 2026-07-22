package ua.com.bravi.bravi.seller.component;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.access.api.AccessContextView;
import ua.com.bravi.bravi.access.api.CurrentAccountHolder;

/**
 * Resolves the internal seller account id for an account-scoped request. The seller-context
 * interceptor has already populated {@link CurrentAccountHolder} from the path {@code accountPublicId}
 * (validating ACTIVE membership); this guard additionally verifies the path matches the current
 * context and the account is a SELLER account, yielding 403 otherwise.
 */
@Component
@RequiredArgsConstructor
public class SellerAccountResolver {

    private static final String SELLER = "SELLER";

    private final CurrentAccountHolder currentAccountHolder;

    public Long resolveSellerAccountId(String accountPublicId) {
        AccessContextView context = currentAccountHolder.getContext()
                .orElseThrow(() -> new AccessDeniedException("No access to this account"));
        if (!context.accountPublicId().equals(accountPublicId)) {
            throw new AccessDeniedException("Account does not match the current context");
        }
        if (!SELLER.equals(context.accountType())) {
            throw new AccessDeniedException("Not a seller account");
        }
        return context.accountId();
    }
}
