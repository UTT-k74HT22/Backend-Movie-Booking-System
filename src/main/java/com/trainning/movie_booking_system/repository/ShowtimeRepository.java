package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // ============================
    // EXIST CHECKS
    // ============================

    boolean existsByScreenIdAndShowDateAndStartTime(
            Long screenId,
            LocalDate showDate,
            LocalTime startTime);

    boolean existsByScreenIdAndShowDateAndStartTimeAndIdNot(
            Long screenId,
            LocalDate showDate,
            LocalTime startTime,
            Long id);

    /**
     * Check if showtimes for the same movie already exist on this screen for this date.
     */
    boolean existsByScreenIdAndShowDateAndMovieId(
            Long screenId,
            LocalDate showDate,
            Long movieId);


    // ============================
    // FIND BY SCREEN
    // ============================

    /**
     * Find all showtimes for a specific screen and date.
     */
    List<Showtime> findByScreenIdAndShowDate(
            Long screenId,
            LocalDate showDate);


    // ============================
    // FIND BY THEATER
    // ============================

    /**
     * Find movies being shown in a specific theater on a specific date.
     */
    @Query("""
        SELECT DISTINCT s.movie
        FROM Showtime s
        JOIN s.screen sc
        JOIN sc.theater t
        WHERE t.id = :theaterId
          AND s.showDate = :date
          AND s.status = 'ACTIVE'
    """)
    List<Movie> findMoviesByTheaterAndDate(
            @Param("theaterId") Long theaterId,
            @Param("date") LocalDate date);


    /**
     * Find showtimes for a specific theater, movie, and date.
     */
    @Query("""
        SELECT s
        FROM Showtime s
        JOIN FETCH s.screen sc
        JOIN FETCH sc.theater t
        WHERE t.id = :theaterId
          AND s.movie.id = :movieId
          AND s.showDate = :date
          AND s.status = 'ACTIVE'
        ORDER BY sc.name, s.startTime
    """)
    List<Showtime> findShowtimesByTheaterAndMovieAndDate(
            @Param("theaterId") Long theaterId,
            @Param("movieId") Long movieId,
            @Param("date") LocalDate date);
}
