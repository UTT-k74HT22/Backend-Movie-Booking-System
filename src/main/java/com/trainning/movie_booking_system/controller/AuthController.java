package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * User sử dụng đăng kí tài khooản mới
     * @param request thông tin cá nhân
     * @return message
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[AUTH] API register username: {}", request.getUsername());
        authService.register(request);
        return ResponseEntity.ok(BaseResponse.success());
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody @Valid VerifyOtpRequest request) {
        log.info("[AUTH] API activate email: {}", request.getEmail());
        authService.activateAccount(request);
        return ResponseEntity.ok(BaseResponse.success("Account activated successfully"));
    }


    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("[AUTH] API login username: {}", request.getUsername());
        return ResponseEntity.ok(BaseResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<String>> test() {
        log.info("[AUTH] API test request");
        return ResponseEntity.ok(BaseResponse.success());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestHeader("Authorization") String refreshTokenHeader) {
        String refreshToken = refreshTokenHeader.replace("Bearer ", "");
        Map<String, String> tokens = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokens);
    }
}
