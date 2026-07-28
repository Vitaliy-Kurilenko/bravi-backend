package ua.com.bravi.bravi.seller.catalog.manufacturers.exception;

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
public class ManufacturersExceptionHandler {

    @ExceptionHandler(ManufacturerAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleManufacturerAlreadyExists(ManufacturerAlreadyExistsException ex) {
        log.debug("ManufacturerAlreadyExistsException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Manufacturer already exists");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(InvalidManufacturerRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequest(InvalidManufacturerRequestException ex) {
        log.debug("InvalidManufacturerRequestException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }
}
