package io.github.tomaszfranczak.batchprocessor.web;

import io.github.tomaszfranczak.batchprocessor.transaction.TransactionPublishException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = exception.getBody();
        problem.setProperty("errors", fieldErrors(exception));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(TransactionPublishException.class)
    ProblemDetail handlePublishFailure(TransactionPublishException exception) {
        log.warn(exception.getMessage(), exception);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "The transaction could not be accepted, try again later");
    }

    private static Map<String, String> fieldErrors(MethodArgumentNotValidException exception) {
        return exception.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first + "; " + second,
                        TreeMap::new));
    }
}
