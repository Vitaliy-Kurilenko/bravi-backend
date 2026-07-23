package ua.com.bravi.bravi.access.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ua.com.bravi.bravi.access.api.AccessApi;
import ua.com.bravi.bravi.access.api.AccountContext;
import ua.com.bravi.bravi.shared.common.HttpConstants;

/**
 * Resolves the request-target account from the {@code X-Account-Id} header (account public id) and
 * populates {@link AccountContext} before {@code @PreAuthorize} runs. A present header with no ACTIVE
 * membership (or unknown account) yields 403; an absent header leaves the context empty, so
 * {@code hasPermission} denies (fail-closed).
 */
@Component
@RequiredArgsConstructor
public class AccountContextInterceptor implements HandlerInterceptor {

    private final AccessApi accessApi;
    private final AccountContext accountContext;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String accountPublicId = request.getHeader(HttpConstants.ACCOUNT_ID_HEADER);
        if (!StringUtils.hasText(accountPublicId)) {
            return true;
        }
        accountContext.set(accessApi.resolveContext(accountPublicId)
                .orElseThrow(() -> new AccessDeniedException("No access to this account")));
        return true;
    }
}
