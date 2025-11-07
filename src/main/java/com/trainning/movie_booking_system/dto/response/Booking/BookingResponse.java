package com.trainning.movie_booking_system.dto.response.Booking;

import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long accountId;
    private Long showtimeId;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private LocalDateTime bookingDate;
    private List<BookingSeatResponse> seats;
}
