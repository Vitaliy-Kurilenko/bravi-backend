package ua.com.bravi.bravi.seller.account.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class SellerAccountExceptionHandler {

    @ExceptionHandler(SellerAccountAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleSellerAccountAlreadyExists(SellerAccountAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Seller account already exists");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
