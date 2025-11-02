package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId")
    List<Seat> findByScreenId(@Param("screenId") Long screenId);

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.status = :status")
    List<Seat> findByScreenIdAndStatus(@Param("screenId") Long screenId, @Param("status") SeatStatus status);

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.rowLabel = :rowLabel AND s.seatNumber = :seatNumber")
    Optional<Seat> findByScreenIdAndRowLabelAndSeatNumber(
            @Param("screenId") Long screenId,
            @Param("rowLabel") String rowLabel,
            @Param("seatNumber") int seatNumber
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seat s WHERE s.screen.id = :screenId AND s.rowLabel = :rowLabel AND s.seatNumber = :seatNumber")
    boolean existsByScreenIdAndRowLabelAndSeatNumber(
            @Param("screenId") Long screenId,
            @Param("rowLabel") String rowLabel,
            @Param("seatNumber") int seatNumber
    );

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId ORDER BY s.rowLabel, s.seatNumber")
    List<Seat> findAllByScreenIdOrderByRowLabelAndSeatNumber(@Param("screenId") Long screenId);
}
