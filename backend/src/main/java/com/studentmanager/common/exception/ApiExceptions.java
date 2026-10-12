package com.studentmanager.common.exception;

/**
 * Domain exceptions, mapped to HTTP responses in GlobalExceptionHandler.
 * Grouped in one file for Phase 4; split out as the API grows.
 */
public final class ApiExceptions {
    private ApiExceptions() {}

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }
}
