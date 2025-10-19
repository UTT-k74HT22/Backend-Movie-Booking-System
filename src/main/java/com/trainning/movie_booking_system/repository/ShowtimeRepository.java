package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    /**
     * Check if a showtime exists for a given screen, date, and start time.
     *
     * @param screenId the ID of the screen
     * @param showDate the date of the show
     * @param startTime the start time of the show
     * @return true if a showtime exists, false otherwise
     */
    boolean existsByScreenIdAndShowDateAndStartTime(Long screenId, LocalDate showDate, LocalTime startTime);

}
