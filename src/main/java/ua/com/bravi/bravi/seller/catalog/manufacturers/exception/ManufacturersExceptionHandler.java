package ua.com.bravi.bravi.seller.catalog.manufacturers.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.com.bravi.bravi.shared.exception.dto.FiledValidationError;

import java.util.List;

@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class ManufacturersExceptionHandler {

    @ExceptionHandler(ManufacturerAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleManufacturerAlreadyExists(ManufacturerAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Manufacturer already exists");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(InvalidManufacturerRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequest(InvalidManufacturerRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }
}
