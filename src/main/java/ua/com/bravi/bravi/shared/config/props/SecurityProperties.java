package ua.com.bravi.bravi.shared.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security-related settings that vary between environments.
 *
 * @param internalRole authority a token must carry to reach {@code /internal/**} (the Auth Service's
 *                     Keycloak service-account role). Defaults to {@code service_registration}.
 */
@ConfigurationProperties(prefix = "bravi.security")
public record SecurityProperties(String internalRole) {

    public SecurityProperties {
        if (internalRole == null || internalRole.isBlank()) {
            internalRole = "registration.write";
        }
    }
}
