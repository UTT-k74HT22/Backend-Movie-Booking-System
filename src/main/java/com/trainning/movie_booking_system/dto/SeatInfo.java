package com.trainning.movie_booking_system.dto;

import com.trainning.movie_booking_system.untils.enums.SeatType;
import lombok.Data;

@Data
public class SeatInfo {
    private Long seatId;
    private SeatType seatType;
}
