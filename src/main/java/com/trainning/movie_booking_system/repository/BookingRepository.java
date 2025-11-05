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
     * Find booked seat IDs for a given showtime and list of seat IDs with specified booking statuses.
     *
     * @param showtimeId the ID of the showtime
     * @param statuses   the list of booking statuses to filter by
     * @param seatIds    the list of seat IDs to check
     * @return a list of booked seat IDs
     */
    @Query("SELECT bs.seatId FROM Booking b JOIN b.bookingSeats bs WHERE b.showtime.id = :showtimeId AND b.status IN :statuses AND bs.seatId IN :seatIds")
    List<Long> findBookedSeatIds(@Param("showtimeId") Long showtimeId,
                                 @Param("statuses") List<BookingStatus> statuses,
                                 @Param("seatIds") List<Long> seatIds);


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

