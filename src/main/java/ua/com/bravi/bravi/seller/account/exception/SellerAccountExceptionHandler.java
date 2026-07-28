package ua.com.bravi.bravi.seller.account.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class SellerAccountExceptionHandler {

    @ExceptionHandler(RegistrationContextConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleRegistrationContextConflict(RegistrationContextConflictException ex) {
        log.debug("RegistrationContextConflictException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Registration context conflict");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
