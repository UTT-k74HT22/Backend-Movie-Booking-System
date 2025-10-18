package com.trainning.movie_booking_system.dto.response.Theater;

import com.trainning.movie_booking_system.untils.enums.TheaterStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TheaterResponse {
    private Long id;
    private String name;
    private String location;
    private String city;
    private String phone;
    private TheaterStatus status;
}
