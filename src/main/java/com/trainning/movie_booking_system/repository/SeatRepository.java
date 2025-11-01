package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    /**
     * Find all seats by screen ID
     *
     * @param screenId the ID of the screen
     * @return list of seats in the screen
     */
    List<Seat> findByScreenId(Long screenId);

    /**
     * Find all seats by screen ID and status
     *
     * @param screenId the ID of the screen
     * @param status the status of the seat
     * @return list of seats matching the criteria
     */
    List<Seat> findByScreenIdAndStatus(Long screenId, SeatStatus status);

    /**
     * Check if a seat exists by screen ID, row label, and seat number
     *
     * @param screenId the ID of the screen
     * @param rowLabel the row label
     * @param seatNumber the seat number
     * @return true if seat exists, false otherwise
     */
    boolean existsByScreenIdAndRowLabelAndSeatNumber(Long screenId, String rowLabel, Integer seatNumber);

    /**
     * Check if a seat exists by screen ID, row label, and seat number, excluding a specific seat ID
     *
     * @param screenId the ID of the screen
     * @param rowLabel the row label
     * @param seatNumber the seat number
     * @param id the ID of the seat to exclude
     * @return true if seat exists, false otherwise
     */
    boolean existsByScreenIdAndRowLabelAndSeatNumberAndIdNot(Long screenId, String rowLabel, Integer seatNumber, Long id);

    /**
     * Find a seat by screen ID, row label, and seat number
     *
     * @param screenId the ID of the screen
     * @param rowLabel the row label
     * @param seatNumber the seat number
     * @return optional seat
     */
    Optional<Seat> findByScreenIdAndRowLabelAndSeatNumber(Long screenId, String rowLabel, Integer seatNumber);
}
