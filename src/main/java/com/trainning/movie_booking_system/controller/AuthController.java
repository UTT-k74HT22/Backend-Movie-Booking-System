package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.dto.response.system.BaseResponse;
import com.trainning.movie_booking_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.ok(BaseResponse.<String>builder()
                    .success(true)
                    .message("Đăng ký thành công")
                    .data("Tài khoản đã được tạo thành công")
                    .build());
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(BaseResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.login(request);
            return ResponseEntity.ok(BaseResponse.<AuthResponse>builder()
                    .success(true)
                    .message("Đăng nhập thành công")
                    .data(authResponse)
                    .build());
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(BaseResponse.<AuthResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * Test endpoint - chỉ dành cho user đã đăng nhập
     */
    @GetMapping("/test")
    public ResponseEntity<BaseResponse<String>> test() {
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .message("JWT authentication thành công!")
                .data("Bạn đã đăng nhập thành công")
                .build());
    }
}
