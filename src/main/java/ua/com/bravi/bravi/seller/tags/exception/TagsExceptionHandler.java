package ua.com.bravi.bravi.seller.tags.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.com.bravi.bravi.shared.exception.dto.FiledValidationError;

import java.util.List;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class TagsExceptionHandler {

    @ExceptionHandler(TagAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleTagAlreadyExists(TagAlreadyExistsException ex) {
        log.debug("TagAlreadyExistsException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Tag already exists");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }

    @ExceptionHandler(InvalidTagRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequest(InvalidTagRequestException ex) {
        log.debug("InvalidTagRequestException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }
}
