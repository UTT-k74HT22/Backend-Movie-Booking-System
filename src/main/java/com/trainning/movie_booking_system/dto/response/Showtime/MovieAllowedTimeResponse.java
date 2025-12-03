package com.trainning.movie_booking_system.dto.response.Showtime;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class MovieAllowedTimeResponse {
    private Long id;
    private Long movieId;
    private LocalTime startTime;
    private LocalTime endTime;
}