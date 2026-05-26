package ua.com.bravi.bravi.config.restclient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ua.com.bravi.bravi.config.props.KeycloakProperties;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakRestClientConfig {

    @Bean
    public RestClient keycloakRestClient(KeycloakProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

}
