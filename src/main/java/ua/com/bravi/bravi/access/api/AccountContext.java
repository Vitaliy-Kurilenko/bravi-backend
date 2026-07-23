package ua.com.bravi.bravi.access.api;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

/**
 * Request-scoped bridge to the current user's authorization context for the account
 * addressed by the {@code X-Account-Id} header. Populated by the account-context interceptor
 * (which resolves the header public id and validates the user's ACTIVE membership) before
 * {@code @PreAuthorize} runs. Empty until set → {@code hasPermission} denies (fail-closed).
 */
@Component
@RequestScope
public class AccountContext {

    private AccessContextView context;

    public Optional<AccessContextView> getContext() {
        return Optional.ofNullable(context);
    }

    public Long getAccountId() {
        return getContext().map(AccessContextView::accountId).orElse(null);
    }

    public boolean hasPermission(String permissionCode) {
        return getContext()
                .map(ctx -> ctx.permissionCodes().contains(permissionCode))
                .orElse(false);
    }

    public void set(AccessContextView context) {
        this.context = context;
    }

    public void invalidate() {
        this.context = null;
    }
}
