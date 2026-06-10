package ua.com.bravi.bravi.shared.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailAuthenticationEntryPointTest {

    private ObjectMapper objectMapper;
    private ProblemDetailAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
        entryPoint = new ProblemDetailAuthenticationEntryPoint(objectMapper);
        request = new MockHttpServletRequest("GET", "/users/test");
        response = new MockHttpServletResponse();
    }

    @Test
    void respondsWithTokenExpired_whenOAuth2ErrorDescriptionMentionsExpired() throws Exception {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "An error occurred while attempting to decode the Jwt: Jwt expired at 2024-01-01T00:00:00Z",
                null
        );

        entryPoint.commence(request, response, new OAuth2AuthenticationException(error));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("title").asString()).isEqualTo("Token expired");
        assertThat(body.get("detail").asString()).isEqualTo("JWT token has expired");
        assertThat(body.get("reason").asString()).isEqualTo("token_expired");
    }

    @Test
    void respondsWithTokenInvalid_whenOAuth2ErrorDescriptionIsMalformed() throws Exception {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "An error occurred while attempting to decode the Jwt: Malformed token",
                null
        );

        entryPoint.commence(request, response, new OAuth2AuthenticationException(error));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("title").asString()).isEqualTo("Invalid token");
        assertThat(body.get("detail").asString()).isEqualTo("JWT token is invalid");
        assertThat(body.get("reason").asString()).isEqualTo("token_invalid");
    }

    @Test
    void respondsWithTokenInvalid_whenOAuth2ErrorDescriptionIsNull() throws Exception {
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN);

        entryPoint.commence(request, response, new OAuth2AuthenticationException(error));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("title").asString()).isEqualTo("Invalid token");
        assertThat(body.get("reason").asString()).isEqualTo("token_invalid");
    }

    @Test
    void matchesExpiredCaseInsensitively() throws Exception {
        OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "JWT EXPIRED",
                null
        );

        entryPoint.commence(request, response, new OAuth2AuthenticationException(error));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("reason").asString()).isEqualTo("token_expired");
    }

    @Test
    void respondsWithGenericUnauthorized_whenNotOAuth2Exception() throws Exception {
        AuthenticationException ex = new InsufficientAuthenticationException("Full authentication is required");

        entryPoint.commence(request, response, ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("title").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("detail").asString()).isEqualTo("Authentication required");
        assertThat(body.has("reason")).isFalse();
    }
}
