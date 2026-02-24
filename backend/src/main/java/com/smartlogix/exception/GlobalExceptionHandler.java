package com.smartlogix.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link IllegalStateException} thrown when a business rule is violated.
     * <p>
     * Typical triggers: invalid order status transitions, duplicate user registration, or
     * missing tenant context in batch processing.
     * </p>
     *
     * @param ex the exception carrying the violation message
     * @return a {@code 400 Bad Request} response containing an {@link ErrorResponse}
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), Instant.now()));
    }

    /**
     * Handles {@link ResourceNotFoundException} thrown when a requested entity does not exist
     * or does not belong to the current tenant.
     *
     * @param ex the exception carrying the resource type and identifier
     * @return a {@code 404 Not Found} response containing an {@link ErrorResponse}
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), Instant.now()));
    }

    /**
     * Handles Jakarta Bean Validation failures raised by {@code @Valid}-annotated controller
     * method parameters.
     * <p>
     * Collects all field-level constraint violations and returns them as a map of
     * {@code fieldName → errorMessage} so that API consumers can pinpoint exactly which
     * fields failed validation.
     * </p>
     *
     * @param ex the exception containing the binding result with field errors
     * @return a {@code 400 Bad Request} response containing a {@link ValidationErrorResponse}
     *         with per-field error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(fieldName, message);
        });
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed", Instant.now(), fieldErrors));
    }

    /**
     * Catch-all handler for any unhandled {@link Exception} that escapes the more specific
     * handlers above.
     * <p>
     * The full stack trace is logged at {@code ERROR} level for debugging. The client receives
     * a generic error message to avoid leaking internal implementation details.
     * </p>
     *
     * @param ex the unhandled exception
     * @return a {@code 500 Internal Server Error} response containing a generic {@link ErrorResponse}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", Instant.now()));
    }

    public record ErrorResponse(int status, String message, Instant timestamp) {}

    public record ValidationErrorResponse(int status, String message, Instant timestamp, Map<String, String> fieldErrors) {}
}
