package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.LogoutRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RefreshTokenRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ForgotPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Auth.ResetPasswordRequest;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.ApiResponse;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.exception.BadRequestException;
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

    /**
     * Active tài khoản
     * @param request thông tin request
     * @return success
     */
    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody @Valid VerifyOtpRequest request) {
        log.info("[AUTH] API activate email: {}", request.getEmail());
        authService.activateAccount(request);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Login
     * @param request username/password
     * @return access and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("[AUTH] API login username: {}", request.getUsername());
        return ResponseEntity.ok(BaseResponse.success(authService.login(request)));
    }

    /**
     * Test authen
     * @return success
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<String>> test() {
        log.info("[AUTH] API test request");
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Refresh token
     * @param request refresh token
     * @return new access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        log.info("[AUTH] API refresh");
        return ResponseEntity.ok(BaseResponse.success(authService.refreshToken(request.getRefreshToken())));
    }
    /**
        * Logout
     * @param request refresh token
     * @return message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(
            @Valid @RequestBody LogoutRequest request) {

        try {
            authService.logout(request.getRefreshToken());
            log.info("User logged out successfully");

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Đăng xuất thành công")
            );
        } catch (BadRequestException e) {
            log.warn("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Có lỗi xảy ra, vui lòng thử lại"));
        }
    }
    /**
    *  Forgot password
    *
    * */
    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[AUTH] API forgot password for email: {}", request.getEmail());
        authService.forgotPassword(request);
        return ResponseEntity.ok(BaseResponse.success());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("[AUTH] API reset password for email: {}", request.getEmail());
        authService.resetPassword(request);
        return ResponseEntity.ok(BaseResponse.success());
    }
}
