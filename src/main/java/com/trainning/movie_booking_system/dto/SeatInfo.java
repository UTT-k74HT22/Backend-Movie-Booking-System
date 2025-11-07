package com.trainning.movie_booking_system.dto;

import com.trainning.movie_booking_system.untils.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatInfo {
    private Long seatId;
    private SeatType seatType;
}
