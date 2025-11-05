package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /**
     * Delete booking seats by booking ID.
     *
     * @param id the ID of the booking
     */
    void deleteByBookingId(Long id);
}
