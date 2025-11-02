package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<BookingRequest, Long> {
}
