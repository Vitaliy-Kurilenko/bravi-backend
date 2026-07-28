package ua.com.bravi.bravi.dictionaries.exception;

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
public class DictionariesExceptionHandler {

    @ExceptionHandler(DictionaryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleDictionaryNotFound(DictionaryNotFoundException ex) {
        log.debug("DictionaryNotFoundException: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Dictionary not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
