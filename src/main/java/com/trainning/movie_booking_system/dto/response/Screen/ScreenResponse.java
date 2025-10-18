package com.trainning.movie_booking_system.dto.response.Screen;

import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScreenResponse {
    private Long id;
    private String name;
    private Integer totalSeats;
    private String status;
    private TheaterResponse theaterResponse;
}
