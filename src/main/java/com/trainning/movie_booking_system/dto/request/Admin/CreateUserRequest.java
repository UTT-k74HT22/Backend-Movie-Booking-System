package com.trainning.movie_booking_system.dto.request.Admin;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    private Boolean isStaff; // true = staff, false = user
}

