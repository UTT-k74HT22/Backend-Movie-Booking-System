package com.trainning.movie_booking_system.exception;

import com.trainning.movie_booking_system.dto.request.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex, WebRequest request) {
        log.error("AppException occurred: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        if (ex.getArgs() != null && ex.getArgs().length > 0) {
            message = String.format(message, ex.getArgs());
        }
        
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(message)
                .result(null)
                .build();
        
        return ResponseEntity.status(ex.getErrorCode().getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error occurred: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message("Validation failed")
                .result(errors)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("IllegalArgumentException occurred: {}", ex.getMessage(), ex);
        
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .result(null)
                .build();
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(false)
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .result(null)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
