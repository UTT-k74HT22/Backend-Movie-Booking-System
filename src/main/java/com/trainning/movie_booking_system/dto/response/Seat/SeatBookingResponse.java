package com.trainning.movie_booking_system.dto.response.Seat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatBookingResponse {

    private Long id;
    private int seatNumber;
    private String rowLabel;
}
