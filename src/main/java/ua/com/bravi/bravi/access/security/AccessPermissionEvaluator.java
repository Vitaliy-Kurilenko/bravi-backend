package ua.com.bravi.bravi.access.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.access.api.AccountContext;

import java.io.Serializable;

/**
 * DB-backed permission evaluator: resolves {@code hasPermission(resource, action)}
 * against the current account's permission set. Wired into method security via the
 * {@code MethodSecurityExpressionHandler} bean (SecurityConfig), so seller controllers
 * gate each endpoint with {@code @PreAuthorize("hasPermission('STORE','WRITE')")} etc.
 */
@Component
@RequiredArgsConstructor
public class AccessPermissionEvaluator implements PermissionEvaluator {

    private final AccountContext accountContext;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject == null || permission == null) {
            return false;
        }
        return accountContext.hasPermission(toCode(targetDomainObject.toString(), permission.toString()));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (targetType == null || permission == null) {
            return false;
        }
        return accountContext.hasPermission(toCode(targetType, permission.toString()));
    }

    private String toCode(String resource, String action) {
        return (resource + "_" + action).toUpperCase();
    }
}
