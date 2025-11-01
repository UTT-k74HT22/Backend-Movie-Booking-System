package com.trainning.movie_booking_system.dto.response.Seat;

import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import com.trainning.movie_booking_system.untils.enums.SeatType;
import lombok.*;

@Getter
@Builder
public class SeatResponse {
    private Long id;
    private int seatNumber;
    private String rowLabel;
    private SeatType seatType;
    private SeatStatus status;
    private Long screenId;
    private String screenName;
}
