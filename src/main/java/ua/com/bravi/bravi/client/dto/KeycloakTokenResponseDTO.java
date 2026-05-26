package ua.com.bravi.bravi.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakTokenResponseDTO(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("refresh_expires_in")
        long refreshExpiresIn,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("not-before-policy")
        int notBeforePolicy,

        @JsonProperty("session_state")
        String sessionState,

        String scope
) { }
