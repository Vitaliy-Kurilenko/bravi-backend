package ua.com.bravi.bravi.access.api;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

/**
 * Request-scoped bridge to the current user's authorization context, so
 * presentation modules and the permission evaluator can read the active
 * account without re-resolving it per call. Mirrors CurrentStoreHolder.
 */
@Component
@RequestScope
public class CurrentAccountHolder {

    private final AccessApi accessApi;

    private AccessContextView context;
    private boolean resolved;

    public CurrentAccountHolder(@Lazy AccessApi accessApi) {
        this.accessApi = accessApi;
    }

    public Optional<AccessContextView> getContext() {
        if (!resolved) {
            context = accessApi.resolveCurrentContext().orElse(null);
            resolved = true;
        }
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
}
