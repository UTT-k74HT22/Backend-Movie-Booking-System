package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Auth.RegisterRequest;

public interface AuthService {
    /**
     * User tạo tào khoản mới
     * @param request thông tin cá nhân
     */
    void register(RegisterRequest request);
}
