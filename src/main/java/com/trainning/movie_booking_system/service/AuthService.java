package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Auth.LoginRequest;
import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;
import com.trainning.movie_booking_system.dto.request.Otp.VerifyOtpRequest;
import com.trainning.movie_booking_system.dto.response.Auth.AuthResponse;

public interface AuthService {
    /**
     * User tạo tài khoản mới
     * @param request thông tin cá nhân
     */
    void register(RegisterRequest request);

    /**
     * Active email
     * @param request thông tin request
     */
    void activateAccount(VerifyOtpRequest request);

    /**
     * User đăng nhập
     * @param request thông tin đăng nhập
     * @return AuthResponse chứa access token và refresh token
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refresh token lấy access token mới
     * @param refreshToken refresh token
     * @return Access and refresh token
     */
    AuthResponse refreshToken(String refreshToken);
}
