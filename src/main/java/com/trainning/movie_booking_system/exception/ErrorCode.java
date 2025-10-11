package com.trainning.movie_booking_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Account related errors
    ACCOUNT_NOT_FOUND(1001, "Account not found", HttpStatus.NOT_FOUND),
    ACCOUNT_ALREADY_EXISTS(1002, "Account already exists", HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_TAKEN(1003, "Username already taken", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_REGISTERED(1004, "Email already registered", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1005, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(1006, "Account is disabled", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(1007, "Account is locked", HttpStatus.FORBIDDEN),
    
    // Authentication & Authorization errors
    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2002, "You do not have permission", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED(2003, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2004, "Invalid token", HttpStatus.UNAUTHORIZED),
    
    // Role related errors
    ROLE_NOT_FOUND(3001, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_EXISTS(3002, "Role already exists", HttpStatus.BAD_REQUEST),
    ROLE_IN_USE(3003, "Role is in use and cannot be deleted", HttpStatus.BAD_REQUEST),
    
    // Permission related errors
    PERMISSION_NOT_FOUND(4001, "Permission not found", HttpStatus.NOT_FOUND),
    PERMISSION_ALREADY_EXISTS(4002, "Permission already exists", HttpStatus.BAD_REQUEST),
    PERMISSION_IN_USE(4003, "Permission is in use and cannot be deleted", HttpStatus.BAD_REQUEST),
    
    // Account-Role related errors
    ACCOUNT_ROLE_NOT_FOUND(5001, "Account role not found", HttpStatus.NOT_FOUND),
    ACCOUNT_ROLE_ALREADY_EXISTS(5002, "Account role already exists", HttpStatus.BAD_REQUEST),
    
    // Role-Permission related errors
    ROLE_PERMISSION_NOT_FOUND(6001, "Role permission not found", HttpStatus.NOT_FOUND),
    ROLE_PERMISSION_ALREADY_EXISTS(6002, "Role permission already exists", HttpStatus.BAD_REQUEST),
    
    // Validation errors
    INVALID_INPUT(7001, "Invalid input data", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(7002, "Missing required field: {field}", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT(7003, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT(7004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_FORMAT(7005, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    
    // Database errors
    DATABASE_ERROR(8001, "Database error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    CONSTRAINT_VIOLATION(8002, "Data constraint violation", HttpStatus.BAD_REQUEST),
    
    // External service errors
    EXTERNAL_SERVICE_ERROR(9001, "External service error", HttpStatus.SERVICE_UNAVAILABLE),
    EMAIL_SERVICE_ERROR(9002, "Email service unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
