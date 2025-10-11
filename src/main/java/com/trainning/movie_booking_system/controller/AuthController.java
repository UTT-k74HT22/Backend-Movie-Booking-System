package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.AccountLoginRequest;
import com.trainning.movie_booking_system.dto.request.AccountRegisterRequest;
import com.trainning.movie_booking_system.dto.request.ApiResponse;
import com.trainning.movie_booking_system.dto.response.AccountResponse;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.service.AccountService;
import com.trainning.movie_booking_system.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccountResponse>> register(@Valid @RequestBody AccountRegisterRequest request) {
        AccountResponse accountResponse = accountService.register(request);
        
        return ResponseEntity.ok(ApiResponse.<AccountResponse>builder()
                .success(true)
                .message("Account registered successfully")
                .result(accountResponse)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody AccountLoginRequest request) {
        Account account = accountService.validateLogin(request);
        
        String accessToken = jwtService.generateToken(account);
        String refreshToken = jwtService.generateRefreshToken(account);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("refreshToken", refreshToken);
        responseData.put("account", AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .emailVerified(account.getEmailVerified())
                .status(account.getStatus())
                .build());
        
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Login successful")
                .result(responseData)
                .build());
    }
}
