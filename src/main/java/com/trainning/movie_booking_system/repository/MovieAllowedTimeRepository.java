package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.MovieAllowedTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieAllowedTimeRepository extends JpaRepository<MovieAllowedTime, Long> {
    /**
     * Lấy tất cả khung giờ được phép chiếu của một phim
     */
    List<MovieAllowedTime> findByMovieId(Long movieId);

}
