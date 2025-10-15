package com.trainning.movie_booking_system.exception;

import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandle {

    // BaseException do mình custom
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleBaseException(BaseException ex) {
        log.error("[BaseException] {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(BaseResponse.failure(ex.getMessage(), ex.getErrors()));
    }

    // Lỗi validate body (ví dụ @Valid trong @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        log.error("[ValidationException] Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure("Validation failed", errors));
    }

    // Lỗi thiếu path variable
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<?> handleMissingPathVariable(MissingPathVariableException ex) {
        log.error("[MissingPathVariable] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure("Missing path variable: " + ex.getVariableName(), null));
    }

    // Lỗi thiếu request param
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        log.error("[MissingRequestParam] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure("Missing request parameter: " + ex.getParameterName(), null));
    }

    // Lỗi validate ở level param (vd: @Min, @Max trong query param)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("[ConstraintViolation] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure("Validation failed", ex.getMessage()));
    }

    // Lỗi validate tham số phương thức (vd: @RequestParam @Email, @NotBlank)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter() != null
                    ? result.getMethodParameter().getParameterName()
                    : "parameter";
            String message = result.getResolvableErrors().isEmpty()
                    ? "Validation failure"
                    : result.getResolvableErrors().get(0).getDefaultMessage();
            errors.put(paramName, message);
        });
        log.error("[HandlerMethodValidation] {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure("Validation failed", errors));
    }

    // Lỗi request method không hỗ trợ (vd: POST -> GET)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("[MethodNotSupported] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(BaseResponse.failure("Method not supported: " + ex.getMethod(), null));
    }

    // Lỗi media type không hỗ trợ (vd: Content-Type: text/plain)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.error("[MediaTypeNotSupported] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(BaseResponse.failure("Media type not supported", ex.getContentType()));
    }

    // Lỗi parse JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex) {
        log.error("[MessageNotReadable] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure("Malformed JSON request", null));
    }

    // Lỗi 403 - không có quyền truy cập
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<?> handleAccessDenied(Exception ex) {
        log.warn("[AccessDenied] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.failure("You do not have permission to access this resource", null));
    }

    // Lỗi 404 - API không tồn tại
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNotFound(NoHandlerFoundException ex) {
        log.error("[NotFound] {}", ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.failure("API not found: " + ex.getRequestURL(), null));
    }

    // Lỗi JWT hết hạn
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwtException(ExpiredJwtException ex) {
        log.error("[ExpiredJwtException] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.failure("Token expired, please login again", null));
    }

    // Lỗi chung (catch all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        log.error("[UnhandledException]", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.failure("Internal server error", null));
    }
}
