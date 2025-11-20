package com.trainning.movie_booking_system.dto.request.Admin;


import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean isStaff;
    private Boolean isActive;;
}
