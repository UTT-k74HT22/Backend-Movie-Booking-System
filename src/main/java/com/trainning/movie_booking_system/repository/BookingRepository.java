package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    /**
     * Find all bookings that have expired based on status and expiry time.
     * Uses expiresAt field for accurate expiration checking.
     * 
     * @param status the booking status to filter by (typically PENDING_PAYMENT)
     * @param now the current time to compare against expiresAt
     * @return a list of expired bookings
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :now")
    List<Booking> findAllExpiredBookings(@Param("status") BookingStatus status,
                                         @Param("now") LocalDateTime now);
}

