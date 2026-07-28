package ua.com.bravi.bravi.seller.exception;

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
public class SellerOnboardingExceptionHandler {

    @ExceptionHandler(EmailNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleEmailNotVerified(EmailNotVerifiedException ex) {
        log.debug("EmailNotVerifiedException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Email not verified");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(OnboardingIncompleteException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ProblemDetail handleOnboardingIncomplete(OnboardingIncompleteException ex) {
        log.debug("OnboardingIncompleteException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Onboarding incomplete");
        problem.setDetail(ex.getMessage());
        problem.setProperty("missing", ex.getMissing());
        return problem;
    }

    @ExceptionHandler(StoreAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleStoreAlreadyExists(StoreAlreadyExistsException ex) {
        log.debug("StoreAlreadyExistsException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Store already exists");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
