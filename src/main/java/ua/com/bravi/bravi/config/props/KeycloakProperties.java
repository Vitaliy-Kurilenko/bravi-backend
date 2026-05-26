package ua.com.bravi.bravi.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloack")
public class KeycloakProperties {
    private String baseUrl;
    private String realm;
    private String clientId;
}
