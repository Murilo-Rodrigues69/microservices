package isakids.isa.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(ApiExceptionHandler::formatFieldError)
                .orElse("Requisicao invalida.");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    private static String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
