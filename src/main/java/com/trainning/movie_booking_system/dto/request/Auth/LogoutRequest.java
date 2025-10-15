package com.trainning.movie_booking_system.dto.request.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LogoutRequest {
    @NotBlank(message = "Token not null")
    private String refreshToken;
}
