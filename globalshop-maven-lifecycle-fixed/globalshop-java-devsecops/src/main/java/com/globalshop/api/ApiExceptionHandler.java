package com.globalshop.api;

import com.globalshop.service.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    public record ErrorView(String error, String message, Instant timestamp) {}
    private ResponseEntity<ErrorView> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorView(status.getReasonPhrase(), message, Instant.now()));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorView> business(BusinessException ex) { return build(ex.getStatus(), ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorView> validation(MethodArgumentNotValidException ex) {
        var first = ex.getBindingResult().getFieldErrors().stream().findFirst();
        return build(HttpStatus.BAD_REQUEST, first.map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Invalid request"));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorView> malformed(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid or incomplete request");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorView> other(Exception ex) {
        log.error("Unhandled request error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
