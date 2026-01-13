package com.trainning.movie_booking_system.dto.request.User;

import lombok.Data;

@Data
public class UserProfileRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatar;
}