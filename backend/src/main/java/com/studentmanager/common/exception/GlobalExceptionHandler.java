package com.studentmanager.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record FieldErrorDto(String field, String message) {}

    public record ErrorBody(
            Instant timestamp,
            int status,
            String code,
            String message,
            String requestId,
            List<FieldErrorDto> fieldErrors
    ) {}

    private ErrorBody body(HttpServletRequest req, int status, String code, String message, List<FieldErrorDto> fieldErrors) {
        return new ErrorBody(Instant.now(), status, code, message, UUID.randomUUID().toString(), fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(
                body(req, 400, "VALIDATION_ERROR", "One or more fields are invalid.", errors)
        );
    }

    @ExceptionHandler(ApiExceptions.ConflictException.class)
    public ResponseEntity<ErrorBody> handleConflict(ApiExceptions.ConflictException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                body(req, 409, "CONFLICT", ex.getMessage(), List.of())
        );
    }

    @ExceptionHandler(ApiExceptions.UnauthorizedException.class)
    public ResponseEntity<ErrorBody> handleUnauthorized(ApiExceptions.UnauthorizedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                body(req, 401, "UNAUTHORIZED", ex.getMessage(), List.of())
        );
    }

    @ExceptionHandler(ApiExceptions.ResourceNotFoundException.class)
    public ResponseEntity<ErrorBody> handleNotFound(ApiExceptions.ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                body(req, 404, "NOT_FOUND", ex.getMessage(), List.of())
        );
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest req) {
        String msg = String.valueOf(ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        // The schedule table's overlap rule is a database EXCLUDE
        // constraint (ex_schedule_no_overlap) - that's the one case this
        // maps to 409 rather than 400, since it's a conflict with existing
        // data rather than a malformed request.
        if (msg.contains("ex_schedule_no_overlap")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    body(req, 409, "SCHEDULE_OVERLAP", "This overlaps with another class already on your timetable.", List.of())
            );
        }
        return ResponseEntity.badRequest().body(
                body(req, 400, "VALIDATION_ERROR", "One or more values violate a database rule (check date ranges and mark limits).", List.of())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception ex, HttpServletRequest req) {
        // Deliberately generic: never leak stack traces or internal detail
        // to the client. Full detail still goes to the server logs.
        return ResponseEntity.internalServerError().body(
                body(req, 500, "INTERNAL_ERROR", "Something went wrong. Please try again.", List.of())
        );
    }
}
