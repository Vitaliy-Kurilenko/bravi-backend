package ua.com.bravi.bravi.seller.catalog.discounts.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.com.bravi.bravi.shared.exception.dto.FiledValidationError;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class DiscountsExceptionHandler {

    @ExceptionHandler(DiscountOverlapException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDiscountOverlap(DiscountOverlapException ex) {
        log.debug("DiscountOverlapException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Discount periods overlap");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        if (ex.getConflicting() != null) {
            problem.setProperty("conflict", Map.of(
                    "submitted", describe(ex.getSubmitted()),
                    "conflicting", describe(ex.getConflicting())));
        }
        return problem;
    }

    @ExceptionHandler(InvalidDiscountRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidRequest(InvalidDiscountRequestException ex) {
        log.debug("InvalidDiscountRequestException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("Request contains invalid fields");
        problem.setProperty("errors", List.of(new FiledValidationError(ex.getField(), ex.getMessage())));
        return problem;
    }

    /** Null-tolerant so a side that has no stored row yet still renders its period. */
    private Map<String, Object> describe(DiscountOverlapException.Side side) {
        Map<String, Object> described = new LinkedHashMap<>();
        described.put("index", side.index());
        described.put("public_id", side.publicId());
        described.put("starts_at", side.startsAt());
        described.put("ends_at", side.endsAt());
        return described;
    }
}
