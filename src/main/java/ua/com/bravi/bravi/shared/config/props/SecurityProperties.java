package ua.com.bravi.bravi.shared.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security-related settings that vary between environments.
 *
 * @param internalRole authority a service token must carry to reach {@code /internal/**} (the Auth
 *                     Service's Keycloak service-account client role on {@code backend-service}).
 *                     Defaults to {@code auth_service}.
 * @param userRole     {@code backend-service} client role every human user must carry to reach any
 *                     non-internal endpoint — the coarse "may call the backend" pass. It carries no
 *                     seller/buyer meaning; the vertical and per-resource rights are decided by the
 *                     DB-backed RBAC ({@code hasPermission}). Defaults to {@code bravi_user}.
 */
@ConfigurationProperties(prefix = "bravi.security")
public record SecurityProperties(String internalRole, String userRole) {

    public SecurityProperties {
        if (internalRole == null || internalRole.isBlank()) {
            internalRole = "auth_service";
        }
        if (userRole == null || userRole.isBlank()) {
            userRole = "bravi_user";
        }
    }
}
