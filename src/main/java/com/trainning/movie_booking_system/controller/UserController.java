package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.ApiResponse;
import com.trainning.movie_booking_system.dto.request.UserUpdateRequest;
import com.trainning.movie_booking_system.dto.response.UserResponse;
import com.trainning.movie_booking_system.helpers.JwtHelper;
import com.trainning.movie_booking_system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtHelper jwtHelper;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(HttpServletRequest request) {
        Long accountId = getCurrentAccountId(request);
        UserResponse userResponse = userService.getUserResponse(accountId);
        
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User profile retrieved successfully")
                .result(userResponse)
                .build());
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UserUpdateRequest request, HttpServletRequest httpRequest) {
        Long accountId = getCurrentAccountId(httpRequest);
        UserResponse userResponse = userService.updateUser(accountId, request);
        
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User profile updated successfully")
                .result(userResponse)
                .build());
    }

    private Long getCurrentAccountId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization header missing or invalid");
        }
        
        try {
            return jwtHelper.getAccountIdFromToken(authHeader);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token: " + e.getMessage());
        }
    }
}
