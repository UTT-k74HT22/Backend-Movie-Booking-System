package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.entity.Booking;

public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .bookingDate(booking.getBookingDate())
                .showtimeId(booking.getShowtime().getId())
//                .voucherId(booking.getVoucher() != null ? booking.getVoucher().getId() : null)
                .userId(booking.getUser().getId())
//                .seats(booking.getBookingSeats().stream()
//                        .map(BookingSeatMapper::toResponse)
//                        .collect(Collectors.toList()))
                .build();
    }
}
