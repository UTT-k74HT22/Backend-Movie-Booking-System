package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.entity.Seat;

public class SeatMapper {

    public static SeatResponse toResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .rowLabel(seat.getRowLabel())
                .seatType(seat.getSeatType())
                .status(seat.getStatus())
                .screenId(seat.getScreen().getId())
                .screenName(seat.getScreen().getName())
                .build();
    }
}
