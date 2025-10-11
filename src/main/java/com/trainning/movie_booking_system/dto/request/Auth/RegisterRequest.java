package com.trainning.movie_booking_system.dto.request.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username not blank")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email not blank")
    @Email(message = "Email.....")
    private String email;

    @NotBlank(message = "Password not blank")
    @Size(min = 6, max = 100, message = "Password,......")
    private String password;
}


