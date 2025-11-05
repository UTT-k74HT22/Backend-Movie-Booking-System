package com.trainning.movie_booking_system.dto.response.Booking;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class BookingSeatResponse {
    private Long id;
    private Long seatId;
    private BigDecimal price;
}
