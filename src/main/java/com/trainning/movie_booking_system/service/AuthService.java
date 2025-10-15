package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface AuthService {
    /**
     * User tạo tài khoản mới
     * @param request thông tin cá nhân
     */
    void register(RegisterRequest request);
    
    /**
     * User đăng nhập
     * @param request thông tin đăng nhập
     * @return AuthResponse chứa access token và refresh token
     */
    AuthResponse login(LoginRequest request);

    /**
     * Verify email using OTP code sent to user's email.
     */
    void verifyOtp(String email, String otp);

    @Transactional
    void resendOtp(String email);


    Map<String, String> refreshToken(String refreshToken);

}
