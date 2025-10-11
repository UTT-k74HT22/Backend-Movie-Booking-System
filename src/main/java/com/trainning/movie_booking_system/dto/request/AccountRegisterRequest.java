package com.trainning.movie_booking_system.dto.request;

import com.trainning.movie_booking_system.untils.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    // User information
    @NotBlank
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank
    @Size(min = 10, max = 15)
    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String address;

    private String city;
}


