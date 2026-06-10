package ua.com.bravi.bravi.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import ua.com.bravi.bravi.shared.exception.dto.FiledValidationError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesAccessDenied() {
        ProblemDetail problem = handler.handleAccessDeniedException(
                new AccessDeniedException("x"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Access Denied");
    }

    @Test
    void handlesMissingRequiredHeader() {
        ProblemDetail problem = handler.handleMissingRequiredHeader(
                new MissingRequiredHeaderException("X-Correlation-Id"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Missing required header");
        assertThat(problem.getDetail()).contains("X-Correlation-Id");
        assertThat(problem.getProperties()).containsEntry("header", "X-Correlation-Id");
    }

    @Test
    void handlesUnexpectedException() {
        ProblemDetail problem = handler.handleUnexpectedException(new RuntimeException("boom"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal server error");
    }

    @Test
    void handlesNotFound() {
        ProblemDetail problem = handler.handleNotFound(new NotFoundException("Store not found"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Not found");
        assertThat(problem.getDetail()).isEqualTo("Store not found");
    }

    @Test
    void handlesForbidden() {
        ProblemDetail problem = handler.handleForbidden(
                new ForbiddenException("Only sellers can manage stores"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Forbidden");
        assertThat(problem.getDetail()).isEqualTo("Only sellers can manage stores");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesValidationErrorsAsFieldList() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("user", "email", "must not be blank"),
                new FieldError("user", "firstName", "must not be null")
        ));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Object> result = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = (ProblemDetail) result.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("Validation failed");

        List<FiledValidationError> errors =
                (List<FiledValidationError>) problem.getProperties().get("errors");
        assertThat(errors).containsExactly(
                new FiledValidationError("email", "must not be blank"),
                new FiledValidationError("firstName", "must not be null")
        );
    }
}
