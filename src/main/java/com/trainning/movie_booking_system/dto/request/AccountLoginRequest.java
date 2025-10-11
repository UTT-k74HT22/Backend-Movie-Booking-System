package com.trainning.movie_booking_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLoginRequest {

    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;
}


