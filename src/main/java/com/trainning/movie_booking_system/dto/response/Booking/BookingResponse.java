package com.trainning.movie_booking_system.dto.response.Booking;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String status;
    private BigDecimal totalPrice;
    private LocalDateTime bookingDate;
    private Long showtimeId;
    private Long voucherId;
    private Long userId;
//    private List<BookingSeatResponse> seats;
}
