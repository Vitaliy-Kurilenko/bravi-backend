package ua.com.bravi.bravi.shared.component;

import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ua.com.bravi.bravi.shared.exception.ExpiredJwtException;
import ua.com.bravi.bravi.shared.exception.InvalidJwtException;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String EXPIRED_DESCRIPTION_MARKER = "expired";
    private static final String REASON_PROPERTY = "reason";
    private static final String REASON_TOKEN_EXPIRED = "token_expired";
    private static final String REASON_TOKEN_INVALID = "token_invalid";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        if (authException instanceof OAuth2AuthenticationException oae) {
            OAuth2Error error = oae.getError();
            String description = error != null ? error.getDescription() : null;

            if (description != null
                    && description.toLowerCase(Locale.ROOT).contains(EXPIRED_DESCRIPTION_MARKER)) {
                ExpiredJwtException expired = new ExpiredJwtException(description, oae);
                log.debug("JWT rejected: expired", expired);
                problem.setTitle("Token expired");
                problem.setDetail("JWT token has expired");
                problem.setProperty(REASON_PROPERTY, REASON_TOKEN_EXPIRED);
            } else {
                InvalidJwtException invalid = new InvalidJwtException(
                        description != null ? description : "JWT token is invalid",
                        oae
                );
                log.debug("JWT rejected: invalid", invalid);
                problem.setTitle("Invalid token");
                problem.setDetail("JWT token is invalid");
                problem.setProperty(REASON_PROPERTY, REASON_TOKEN_INVALID);
            }
        } else {
            problem.setTitle("Unauthorized");
            problem.setDetail("Authentication required");
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
