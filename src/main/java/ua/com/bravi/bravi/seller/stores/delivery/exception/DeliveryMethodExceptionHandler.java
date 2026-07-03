package ua.com.bravi.bravi.seller.stores.delivery.exception;

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
public class DeliveryMethodExceptionHandler {

    @ExceptionHandler(UnknownDeliveryMethodException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUnknownDeliveryMethod(UnknownDeliveryMethodException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Delivery method not found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("methodCode", ex.getMethodCode());
        return problem;
    }

    @ExceptionHandler(InvalidDeliveryConfigException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ProblemDetail handleInvalidDeliveryConfig(InvalidDeliveryConfigException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("Invalid delivery configuration");
        problem.setDetail("Delivery method configuration contains invalid fields");
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }
}
