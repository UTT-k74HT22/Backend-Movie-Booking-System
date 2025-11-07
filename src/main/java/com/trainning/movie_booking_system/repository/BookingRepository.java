package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    /**
     * Find all bookings that have expired based on status and expiry time.
     * @param status the booking status to filter by
     * @param expiryTime the expiry time to compare booking dates against
     * @return a list of expired bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingDate < :expiryTime")
    List<Booking> findAllExpiredBookings(@Param("status") BookingStatus status,
                                         @Param("expiryTime") LocalDateTime expiryTime);
}

