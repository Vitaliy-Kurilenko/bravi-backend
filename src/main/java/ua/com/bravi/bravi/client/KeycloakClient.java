package ua.com.bravi.bravi.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import ua.com.bravi.bravi.client.dto.KeycloakTokenResponseDTO;
import ua.com.bravi.bravi.config.props.KeycloakProperties;
import ua.com.bravi.bravi.exception.ExternalServiceException;
import ua.com.bravi.bravi.exception.KeycloakClientException;

@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private final RestClient keycloakRestClient;
    private final KeycloakProperties properties;

    public KeycloakTokenResponseDTO login(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.getClientId());
        formData.add("username", username);
        formData.add("passwloginord", password);

        try {
            return keycloakRestClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KeycloakTokenResponseDTO.class);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) {
            throw new InvalidCredentialsException("Невірний email або пароль");

        } catch (org.springframework.web.client.HttpClientErrorException ex) {

            throw new KeycloakClientException(
                    "Помилка Keycloak: " + ex.getStatusCode()
            );

        } catch (Exception ex) {
            throw new ExternalServiceException("Keycloak недоступний", ex);
        }

    }
}
