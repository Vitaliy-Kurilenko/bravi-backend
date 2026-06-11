package ua.com.bravi.bravi.stores.payments.exception;

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
public class PaymentMethodExceptionHandler {

    @ExceptionHandler(UnknownPaymentMethodException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUnknownPaymentMethod(UnknownPaymentMethodException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Payment method not found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("methodCode", ex.getMethodCode());
        return problem;
    }

    @ExceptionHandler(InvalidPaymentConfigException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ProblemDetail handleInvalidPaymentConfig(InvalidPaymentConfigException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("Invalid payment configuration");
        problem.setDetail("Payment method configuration contains invalid fields");
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }
}
