package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.Booking.BookingSeatResponse;
import com.trainning.movie_booking_system.dto.response.Seat.SeatBookingResponse;
import com.trainning.movie_booking_system.entity.Booking;

import java.util.stream.Collectors;

public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .bookingDate(booking.getBookingDate())
                .showtimeId(booking.getShowtime().getId())
//                .voucherId(booking.getVoucher() != null ? booking.getVoucher().getId() : null)
                .accountId(booking.getAccount().getId())
                .seats(booking.getBookingSeats().stream()
                        .map(BookingMapper::toSeatResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public static BookingSeatResponse toSeatResponse(com.trainning.movie_booking_system.entity.BookingSeat bookingSeat) {
        return BookingSeatResponse.builder()
                .id(bookingSeat.getId())
                .seat(SeatBookingResponse.builder()
                        .id(bookingSeat.getSeat().getId())
                        .seatNumber(bookingSeat.getSeatNumber())
                        .rowLabel(bookingSeat.getRowLabel())
                        .seatType(bookingSeat.getSeatType())
                        .build())
                .price(bookingSeat.getPrice())
                .build();
    }
}
