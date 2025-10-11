package com.trainning.movie_booking_system.dto.response;

import com.trainning.movie_booking_system.untils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String username;
    private String email;
    private Boolean emailVerified;
    private Status status;
}


