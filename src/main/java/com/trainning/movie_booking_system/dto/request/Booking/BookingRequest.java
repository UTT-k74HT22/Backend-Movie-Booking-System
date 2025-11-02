package com.trainning.movie_booking_system.dto.request.Booking;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private Long showtimeId;
    private Long voucherId;
    private List<Long> seatIds;
}
