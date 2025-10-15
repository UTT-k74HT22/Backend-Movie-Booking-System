package com.trainning.movie_booking_system.dto.request.Otp;

import com.trainning.movie_booking_system.untils.enums.OtpType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Email not null")
    private String email;

    @NotBlank(message = "OTP not null")
    private String otp;

    private OtpType type;
}
