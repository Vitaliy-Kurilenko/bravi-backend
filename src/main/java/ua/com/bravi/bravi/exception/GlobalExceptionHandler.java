package ua.com.bravi.bravi.exception;
import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import ua.com.bravi.bravi.exception.dto.FiledValidationError;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDeniedException(
            AccessDeniedException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Access Denied");
        problem.setDetail("Request contains problems");

        return problem;
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        ArrayList<FiledValidationError> errors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FiledValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            ));
        }

        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        ArrayList<FiledValidationError> errors = new ArrayList<>();
        errors.add(new FiledValidationError(extractField(ex), rootMessage(ex)));

        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    private String extractField(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JacksonException jacksonEx && !jacksonEx.getPath().isEmpty()) {
            return jacksonEx.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null
                            ? ref.getPropertyName()
                            : "[" + ref.getIndex() + "]")
                    .reduce((a, b) -> a + "." + b)
                    .orElse(null);
        }
        return null;
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }

    @ExceptionHandler(MissingRequiredHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingRequiredHeader(
            MissingRequiredHeaderException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Missing required header");
        problem.setDetail("Request must include '" + ex.getHeaderName() + "' header");
        problem.setProperty("header", ex.getHeaderName());

        return problem;
    }

    @ExceptionHandler(UserProvisioningException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ProblemDetail handleUserProvisioning(
            UserProvisioningException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("User provisioning failed");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(
            NotFoundException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not found");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(StoreAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleStoreAlreadyExists(
            StoreAlreadyExistsException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Store already exists");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleForbidden(
            ForbiddenException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Forbidden");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleUnexpectedException(
            Exception ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("Unexpected error occurred");

        return problem;
    }

}
